package rv32e

import chisel3._
import chisel3.util._
import chisel3.stage._
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
import rv32e.utils.AxiLiteConnect
import rv32e.dev.SRAM_axi
import rv32e.dev.sram_axi_rw
import rv32e.cache._
import _root_.circt.stage.ChiselStage
import _root_.circt.stage.CIRCTTargetAnnotation
import _root_.circt.stage.CIRCTTarget

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

    val IDU_i   =   Module(new IDU())
    val ISU_i   =   Module(new ISU())
    val EXU_i   =   Module(new EXU()) 
    val WBU_i   =   Module(new WBU())

    /* ifu connect to sram with axi-lite */
    // val IFU_i   =   Module(new IFU())
    // val sram_i  =   Module(new SRAM())
    // AxiLiteConnect(IFU_i.axi, sram_i.axi)

    /* ifu connect to sram with axi */
    // val IFU_i   =   Module(new IFU_axi())
    // val sram_i  =   Module(new SRAM_axi())
    // AxiConnect(IFU_i.axi, sram_i.axi)

    /* ifu connect to cache */
    // val IFU_i   =   Module(new IFU_cache())
    // val icache  =   Module(new iCacheV2())
    val IFU_i   =   Module(new IFU_simpleBus)
    val icache  =   Module(new Icache_SimpleBus())
    val sram_i  =   Module(new sram_axi_rw())
    // StageConnect(IFU_i.to_cache, icache.from_IFU)
    // StageConnect(icache.to_IFU, IFU_i.from_cache)
    IFU_i.to_mem <> icache.from_ifu
    AxiConnect(icache.to_sram, sram_i.axi)
    
    /* lsu connect to mem with axi */
    // val sram_i2 =   Module(new sram_axi_rw())
    // AxiConnect(EXU_i.lsu_axi_master, sram_i2.axi)

    /* lsu connect to cache */
    // val dcache  =   Module(new D_Cache())
    val dcache  =   Module(new Dcache_SimpleBus())
    val sram_i2 =   Module(new sram_axi_rw())
    // StageConnect(EXU_i.lsu_to_cache, dcache.from_LSU)
    // StageConnect(dcache.to_LSU, EXU_i.lsu_from_cache)
    EXU_i.lsu_to_mem <> dcache.from_lsu
    AxiConnect(dcache.to_sram, sram_i2.axi)

    // val sram_i  =   Module(new SRAM())
    // val arbiter_i = Module(new Arbiter())
    // AxiLiteConnect(IFU_i.axi, arbiter_i.from_master1)
    // AxiLiteConnect(EXU_i.lsu_axi_master, arbiter_i.from_master2)
    // AxiLiteConnect(arbiter_i.to_slave, sram_i.axi)

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
    // emitVerilog(new top(), Array("--target-dir", "generated/cpu"))
    // def 
    // (new ChiselStage).execute(args, Seq(ChiselGeneratorAnnotation(() => new top)))
    def t = new top()
    // println(args)
    val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => t))
    (new ChiselStage).execute(args, generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
}
