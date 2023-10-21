package rv32e

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.utils.DiffCsr
import java.awt.MouseInfo
import rv32e.utils.StageConnect
import rv32e.utils.StageConnect_reg
import utils.RegFile
import rv32e.dev.SRAM
import rv32e.bus.Arbiter
import rv32e.utils.AxiConnect

class out_class extends Bundle {
    val inst     = Output(UInt(INST_WIDTH.W))
    val pc       = Output(UInt(DATA_WIDTH.W))
    val difftest = new DiffCsr
    val wb       = Output(Bool())
}

class top extends Module {
    val io = IO(new Bundle{
        val out = (new out_class)
    })

    val IFU_i   =   Module(new IFU())
    val IDU_i   =   Module(new IDU())
    val ISU_i   =   Module(new ISU())
    val EXU_i   =   Module(new EXU()) 
    val WBU_i   =   Module(new WBU())

    val sram_i  =   Module(new SRAM())

    // AxiConnect(IFU_i.axi, sram_i.axi)

    val arbiter_i = Module(new Arbiter())
    AxiConnect(IFU_i.axi, arbiter_i.from_master1)
    AxiConnect(arbiter_i.to_slave, sram_i.axi)


    StageConnect(EXU_i.to_IFU, IFU_i.from_EXU)
    StageConnect(IFU_i.to_IDU, IDU_i.from_IFU)
    StageConnect(IDU_i.to_ISU, ISU_i.from_IDU)
    StageConnect_reg(ISU_i.to_EXU, EXU_i.from_ISU)
    // StageConnect(ISU_i.to_EXU, EXU_i.from_ISU)
    StageConnect(EXU_i.to_WBU, WBU_i.from_EXU)
    StageConnect(WBU_i.to_ISU, ISU_i.from_WBU)
    StageConnect(WBU_i.to_IFU, IFU_i.from_WBU)

    io.out.inst    := IFU_i.to_IDU.bits.inst
    io.out.pc      := IFU_i.to_IDU.bits.pc
    io.out.wb      := WBU_i.to_IFU.valid

    io.out.difftest <> EXU_i.difftest
}

object top_main extends App {
    emitVerilog(new top(), Array("--target-dir", "generated/cpu"))
}
