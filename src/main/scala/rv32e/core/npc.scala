package rv32e.core

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import chisel3.util.experimental.BoringUtils
import chisel3.stage._

import _root_.circt.stage.ChiselStage
import _root_.circt.stage.CIRCTTargetAnnotation
import _root_.circt.stage.CIRCTTarget
import _root_.circt.stage.FirtoolOption

import rv32e.bus._
import rv32e.bus.simplebus._
import rv32e.bus.axi4._

import rv32e.core.backend._
import rv32e.core.frontend._
import rv32e.core.define.Mem._
import rv32e.core.config._
import rv32e.core.mem.cache._

import rv32e.device._

import rv32e.utils._

// npc module
// class npc extends Module {
class top extends Module {
    val io = IO(new Bundle {
        // val master = (new AXI4)
        val out = (new out_class)
    })

    val core_i   = Module(new core())
    val icache   = Module(new Cache(DATA_WIDTH, "icache"))
    val dcache   = Module(new Cache(DATA_WIDTH, "dcache"))
    val clint    = Module(new AXI4CLINT())

    val dcacheAddrRange = (pmemBase, pmemSize)
    val clientAddrRange = (RTC_ADDR, 8L)
    val mmioAddrRange   = (RTC_ADDR+8L, mmioSize)

    val addrSpace = List(
        dcacheAddrRange,
        clientAddrRange,
        mmioAddrRange,
    )
    val xbar1toN = Module(new SimpleBusCrossBar1toN(addrSpace))
    val xbarNto1 = Module(new SimpleBusCrossBarNto1(3))

    /* connect wires according to input, because input only have
        one, and output maybe have many. */

    // input of core
    core_i.io.icachePC := icache.io.stage2Addr
    
    // input of icache
    icache.io.in <> core_i.io.ifu
    icache.io.flush := core_i.io.flush

    // input of xbar1toN
    xbar1toN.io.in <> core_i.io.lsu
    xbar1toN.io.flush := core_i.io.flush

    // input of dcache
    dcache.io.in <> xbar1toN.io.out(0)
    dcache.io.flush := core_i.io.flush
    
    // client
    clint.io.in <> xbar1toN.io.out(1).toAXI4Lite()

    // input of xbarNto1
    xbarNto1.io.in(0) <> icache.io.mem
    xbarNto1.io.in(1) <> dcache.io.mem
    xbarNto1.io.in(2) <> xbar1toN.io.out(2)
    xbarNto1.io.flush := core_i.io.flush

    //////////////// out of npc ////////////////
    val ramAddrRange = (pmemBase, pmemSize)
    val addrSpace2 = List(
        mmioAddrRange,
        ramAddrRange,
    )
    val ram_i    = Module(new AXI4RAM)
    val mmio_i   = Module(new MMIO)
    val xbar1toNout = Module(new SimpleBusCrossBar1toN(addrSpace2))

    xbar1toNout.io.in <> xbarNto1.io.out
    xbar1toNout.io.flush := core_i.io.flush

    mmio_i.io.in <> xbar1toNout.io.out(0) 
    mmio_i.io.flush := core_i.io.flush
    
    ram_i.axi <> xbar1toNout.io.out(1).toAXI4()

    ////////////// for output ///////////////
    io.out <> core_i.io.out
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