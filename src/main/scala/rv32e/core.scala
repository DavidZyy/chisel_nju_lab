package rv32e

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.utils.DiffCsr
import java.awt.MouseInfo
import rv32e.utils.StageConnect
import utils.RegFile
import rv32e.dev.SRAM

class out_class extends Bundle {
    val inst     = Output(UInt(INST_WIDTH.W))
    val pc       = Output(UInt(DATA_WIDTH.W))
    val difftest = new DiffCsr
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
    StageConnect(IFU_i.axi.ar, sram_i.axi.ar)
    StageConnect(sram_i.axi.r, IFU_i.axi.r)

    StageConnect(EXU_i.to_IFU, IFU_i.from_EXU)
    StageConnect(IFU_i.to_IDU, IDU_i.from_IFU)
    StageConnect(IDU_i.to_ISU, ISU_i.from_IDU)
    StageConnect(ISU_i.to_EXU, EXU_i.from_ISU)
    StageConnect(EXU_i.to_WBU, WBU_i.from_EXU)
    StageConnect(WBU_i.to_ISU, ISU_i.from_WBU)

    io.out.inst    := IFU_i.to_IDU.bits.inst
    io.out.pc      := IFU_i.to_IDU.bits.pc

    io.out.difftest <> EXU_i.difftest
}

object top_main extends App {
    emitVerilog(new top(), Array("--target-dir", "generated/cpu"))
}
