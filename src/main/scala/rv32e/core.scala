package rv32e

import chisel3._
import chisel3.util._
import chisel3.stage._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.utils.DiffCsr
import java.awt.MouseInfo
import utils.RegFile
import rv32e.device.SRAM
import rv32e.bus.Arbiter
import rv32e.device.AXI4RAM
import rv32e.cache._
import rv32e.define.Mem._
import _root_.circt.stage.ChiselStage
import _root_.circt.stage.CIRCTTargetAnnotation
import _root_.circt.stage.CIRCTTarget
import rv32e.bus._
import rv32e.device._
import rv32e.utils._
import rv32e.define.Dec_Info
import _root_.circt.stage.FirtoolOption
import chisel3.util.experimental.BoringUtils

class out_class extends Bundle {
    val ifu_fetchPc = Output(UInt(ADDR_WIDTH.W))
    val nextExecPC  = Output(UInt(ADDR_WIDTH.W)) // the next execute pc after a wb signal, for difftest
    val ifu      = new PipelineDebugInfo
    val idu      = new PipelineDebugInfo
    val isu      = new PipelineDebugInfo
    val exu      = new PipelineDebugInfo
    val wbu      = new PipelineDebugInfo
    val difftest = new DiffCsr
    val wb       = Output(Bool())
}

class top extends Module {
    val io = IO(new Bundle{
        val out = (new out_class)
    })

    val IDU_i   =   Module(new IDU())
    val ISU_i   =   Module(new ISU())
    val EXU_i   =   Module(new EXU_pipeline()) 
    val WBU_i   =   Module(new WBU())

    /* ifu connect to cache */
    val IFU_i   =   Module(new IFU_pipeline())
    val ram_i   =   Module(new AXI4RAM())

    // val icache  =   Module(new Icache_pipeline())
    // IFU_i.to_mem    <> icache.from_ifu
    // IFU_i.to_IDU_PC := icache.instPC
    // icache.to_sram  <> ram_i.axi
    // icache.redirect := EXU_i.to_IFU.bits.redirect
    
    val icache  =   Module(new Cache(DATA_WIDTH))
    IFU_i.to_mem  <> icache.io.in
    IFU_i.to_IDU_PC := icache.io.stage2Addr
    icache.io.mem.toAXI4() <> ram_i.axi
    icache.io.flush := WBU_i.to_IFU.bits.redirect.valid

    val ram_i2 =   Module(new AXI4RAM())

    val addrSpace = List(
        (pmemBase, pmemSize),
        (mmioBase, mmioSize),
    )
    val memXbar = Module(new SimpleBusCrossBar1toN(addrSpace))
    val dcache  =   Module(new Dcache_SimpleBus())
    val mmio    =   Module(new MMIO())
    EXU_i.lsu_to_mem  <> memXbar.io.in
    memXbar.io.flush  := WBU_i.to_IFU.bits.redirect.valid
    memXbar.io.out(0) <> dcache.from_lsu
    memXbar.io.out(1) <> mmio.from_lsu
    dcache.to_sram    <> ram_i2.axi

    // EXU_i.lsu_to_mem <> ram_i2.axi

    BoringUtils.addSource(WBU_i.to_IFU.bits.redirect.valid, "id5")

    WBU_i.to_IFU <> IFU_i.from_WBU
    PipelineConnect(IFU_i.to_IDU, IDU_i.from_IFU, IDU_i.to_ISU.fire, WBU_i.to_IFU.bits.redirect.valid)// && IFU_i.to_IDU.fire)
    PipelineConnect(IDU_i.to_ISU, ISU_i.from_IDU, ISU_i.to_EXU.fire, WBU_i.to_IFU.bits.redirect.valid)// && IDU_i.to_ISU.fire)
    PipelineConnect(ISU_i.to_EXU, EXU_i.from_ISU, EXU_i.to_WBU.fire, WBU_i.to_IFU.bits.redirect.valid)// && ISU_i.to_EXU.fire)
    PipelineConnect(EXU_i.to_WBU, WBU_i.from_EXU, WBU_i.wb, WBU_i.to_IFU.bits.redirect.valid)// && EXU_i.to_WBU.fire)
    // EXU_i.to_WBU <> WBU_i.from_EXU
    EXU_i.to_ISU <> ISU_i.from_EXU
    WBU_i.to_ISU <> ISU_i.from_WBU

    val CacheStage2PC    = WireInit(0.U(DATA_WIDTH.W))
    val CacheStage2valid = WireInit(true.B)
    BoringUtils.addSink(CacheStage2valid, "id1")
    BoringUtils.addSink(CacheStage2PC, "id2")

    io.out.ifu_fetchPc := IFU_i.fetch_PC

    when(WBU_i.from_EXU.valid) {
        io.out.nextExecPC := WBU_i.from_EXU.bits.pc
    } .elsewhen(EXU_i.from_ISU.valid) {
        io.out.nextExecPC := EXU_i.from_ISU.bits.pc
    } .elsewhen(ISU_i.from_IDU.valid) {
        io.out.nextExecPC := ISU_i.from_IDU.bits.pc
    } .elsewhen(IDU_i.from_IFU.valid) {
        io.out.nextExecPC := IDU_i.from_IFU.bits.pc
    } .elsewhen(CacheStage2valid) {
        io.out.nextExecPC := CacheStage2PC
    } .otherwise {
        io.out.nextExecPC := IFU_i.fetch_PC
    }
    io.out.ifu.pc   := IFU_i.to_IDU.bits.pc
    io.out.ifu.inst := IFU_i.to_IDU.bits.inst
    io.out.idu.pc   := IDU_i.to_ISU.bits.pc
    io.out.idu.inst := IDU_i.to_ISU.bits.inst
    io.out.isu.pc   := ISU_i.to_EXU.bits.pc
    io.out.isu.inst := ISU_i.to_EXU.bits.inst
    io.out.exu.pc   := EXU_i.to_WBU.bits.pc
    io.out.exu.inst := EXU_i.to_WBU.bits.inst
    io.out.wbu.pc   := WBU_i.from_EXU.bits.pc
    io.out.wbu.inst := WBU_i.from_EXU.bits.inst
    io.out.wb       := WBU_i.wb
    io.out.difftest <> EXU_i.difftest
}

object top_main extends App {
    def t = new top()
    val generator = Seq(
        chisel3.stage.ChiselGeneratorAnnotation(() => t),
        CIRCTTargetAnnotation(CIRCTTarget.Verilog)
    )
    val firtoolOptions = Seq(
        FirtoolOption("--disable-all-randomization"),
        FirtoolOption("--lowering-options=disallowLocalVariables, locationInfoStyle=none"),
        // FirtoolOption("--lowering-options=locationInfoStyle=none")
    )
    (new ChiselStage).execute(args, generator ++ firtoolOptions)
}