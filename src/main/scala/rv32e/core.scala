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
import rv32e.device.SRAM
import rv32e.bus.Arbiter
import rv32e.utils.AxiConnect
import rv32e.utils.AxiLiteConnect
import rv32e.device.SRAM_axi
import rv32e.device.sram_axi_rw
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

class out_class extends Bundle {
    val inst     = Output(UInt(INST_WIDTH.W))
    val pc       = Output(UInt(DATA_WIDTH.W))
    val difftest = new DiffCsr
    val wb       = Output(Bool())
    val wb_inst  = Output(UInt(INST_WIDTH.W)) // for not optimize signal to debug
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
    // val icache  =   Module(new Icache_SimpleBus())
    val icache  =   Module(new Icache_pipeline())
    val sram_i  =   Module(new sram_axi_rw())
    IFU_i.to_mem   <> icache.from_ifu
    icache.to_sram <> sram_i.axi
    
    val addrSpace = List(
        (pmemBase, pmemSize),
        (mmioBase, mmioSize),
    )

    val memXbar   = Module(new SimpleBusCrossBar1toN(addrSpace))

    /* lsu connect to cache */
    val dcache  =   Module(new Dcache_SimpleBus())
    val sram_i2 =   Module(new sram_axi_rw())
    val mmio    =   Module(new MMIO())
    EXU_i.lsu_to_mem  <> memXbar.io.in
    memXbar.io.out(0) <> dcache.from_lsu
    memXbar.io.out(1) <> mmio.from_lsu
    dcache.to_sram    <> sram_i2.axi

    EXU_i.to_IFU <> IFU_i.from_EXU
    IFU_i.to_IDU <> IDU_i.from_IFU
    IDU_i.to_ISU <> ISU_i.from_IDU

    PipelineConnect(ISU_i.to_EXU, EXU_i.from_ISU, EXU_i.to_WBU.fire,  EXU_i.to_IFU.bits.redirect && ISU_i.to_EXU.fire)
    EXU_i.to_WBU <> WBU_i.from_EXU
    WBU_i.to_ISU <> ISU_i.from_WBU

    io.out.inst     := IFU_i.to_IDU.bits.inst
    io.out.pc       := IFU_i.to_IDU.bits.pc
    io.out.wb       := WBU_i.from_EXU.valid
    io.out.wb_inst  := WBU_i.from_EXU.bits.inst
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