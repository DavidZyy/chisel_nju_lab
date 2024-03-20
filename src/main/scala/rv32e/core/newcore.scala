package rv32e.core

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import chisel3.util.experimental.BoringUtils
import chisel3.stage._

import rv32e.bus._
import rv32e.bus.simplebus._

import rv32e.core.backend._
import rv32e.core.frontend._
import rv32e.core.config._

import rv32e.utils._

class core extends Module {
    val io = IO(new Bundle{
        val ifu = (new SimpleBus)
        val lsu = (new SimpleBus)
        val icachePC = Input(UInt(DATA_WIDTH.W)) 
        val flush    = Output(Bool())
        val out = (new out_class)
    })

    val IFU_i   =   Module(new IFU_pipeline())
    val IDU_i   =   Module(new IDU())
    val ISU_i   =   Module(new ISU())
    val EXU_i   =   Module(new EXU_pipeline()) 
    val WBU_i   =   Module(new WBU())

    WBU_i.to_IFU <> IFU_i.from_WBU
    IFU_i.to_IDU_PC := io.icachePC
    PipelineConnect(IFU_i.to_IDU, IDU_i.from_IFU, IDU_i.to_ISU.fire, WBU_i.to_IFU.bits.redirect.valid)// && IFU_i.to_IDU.fire)
    PipelineConnect(IDU_i.to_ISU, ISU_i.from_IDU, ISU_i.to_EXU.fire, WBU_i.to_IFU.bits.redirect.valid)// && IDU_i.to_ISU.fire)
    PipelineConnect(ISU_i.to_EXU, EXU_i.from_ISU, EXU_i.to_WBU.fire, WBU_i.to_IFU.bits.redirect.valid)// && ISU_i.to_EXU.fire)
    PipelineConnect(EXU_i.to_WBU, WBU_i.from_EXU, WBU_i.wb, WBU_i.to_IFU.bits.redirect.valid)// && EXU_i.to_WBU.fire)
    EXU_i.to_ISU <> ISU_i.from_EXU
    EXU_i.npc    := ISU_i.to_EXU.bits.pc
    WBU_i.to_ISU <> ISU_i.from_WBU
    ISU_i.flush  := WBU_i.to_IFU.bits.redirect.valid

    io.ifu <> IFU_i.to_mem
    io.lsu <> EXU_i.lsu_to_mem
    io.flush := WBU_i.to_IFU.bits.redirect.valid

    ////////////// for perf ///////////////
    if (EnablePerfCnt) {
        val PerfCnt_i = Module(new perfCnt())
        BoringUtils.addSource(WireInit(ISU_i.to_EXU.fire && ISU_i.to_EXU.bits.isLSU), perfPrefix+"nrLSU")
        BoringUtils.addSource(WireInit(ISU_i.to_EXU.fire && ISU_i.to_EXU.bits.isBRU), perfPrefix+"nrBRU")
        BoringUtils.addSource(WireInit(ISU_i.to_EXU.fire && ISU_i.to_EXU.bits.isCSR), perfPrefix+"nrCSR")
        BoringUtils.addSource(WireInit(ISU_i.to_EXU.fire && ISU_i.to_EXU.bits.isALU), perfPrefix+"nrALU")
        BoringUtils.addSource(WireInit(ISU_i.to_EXU.fire && ISU_i.to_EXU.bits.isMDU), perfPrefix+"nrMDU")
    }

    ////////////// for output ///////////////
    when(WBU_i.from_EXU.valid) {
        io.out.nextExecPC := WBU_i.from_EXU.bits.pc
    } .elsewhen(EXU_i.from_ISU.valid) {
        io.out.nextExecPC := EXU_i.from_ISU.bits.pc
    } .elsewhen(ISU_i.from_IDU.valid) {
        io.out.nextExecPC := ISU_i.from_IDU.bits.pc
    } .elsewhen(IDU_i.from_IFU.valid) {
        io.out.nextExecPC := IDU_i.from_IFU.bits.pc
    } .elsewhen(IFU_i.to_IDU.valid) {
        io.out.nextExecPC := IFU_i.to_IDU.bits.pc
    } .otherwise {
        io.out.nextExecPC := IFU_i.fetch_PC
    }

    io.out.ifu_fetchPc := IFU_i.fetch_PC
    io.out.ifu.pc   := IFU_i.to_IDU.bits.pc       // actually icache pc
    io.out.ifu.inst := IFU_i.to_IDU.bits.inst
    io.out.idu.pc   := IDU_i.from_IFU.bits.pc
    io.out.idu.inst := IDU_i.from_IFU.bits.inst
    io.out.isu.pc   := ISU_i.from_IDU.bits.pc
    io.out.isu.inst := ISU_i.from_IDU.bits.inst
    io.out.exu.pc   := EXU_i.from_ISU.bits.pc
    io.out.exu.inst := EXU_i.from_ISU.bits.inst
    io.out.wbu.pc   := WBU_i.from_EXU.bits.pc
    io.out.wbu.inst := WBU_i.from_EXU.bits.inst

    io.out.wb       := WBU_i.wb
    io.out.difftest <> EXU_i.difftest
    io.out.is_mmio  := WBU_i.is_mmio
}
