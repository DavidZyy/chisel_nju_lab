package rv32e.bus.axi4

import chisel3._
import chisel3.util._

import rv32e.utils._

import rv32e.core.config._

import rv32e.bus.config._

// object AXI4Parameters {
trait AXI4Parameters {
    // These are all fixed by the AXI4 standard:
    val lenBits   = 8
    val sizeBits  = 3
    val burstBits = 2
    val respBits  = 2

    // These are not fixed:
    val addrBits = ADDR_WIDTH
    val dataBits = DATA_WIDTH

    def BURST_FIXED = 0.U(burstBits.W)
    def BURST_INCR  = 1.U(burstBits.W)
    def BURST_WRAP  = 2.U(burstBits.W)
}

abstract class AXI4Bundle extends Bundle with AXI4Parameters

/**
  * extract the common field to trait
  */
trait AXI4HasData extends AXI4Parameters {
    val data = Output(UInt(dataBits.W))
}

trait AXI4HasLast {
    val last = Output(Bool())
}

// AXI4-lite

class AXI4LiteBundleA extends AXI4Bundle {
    val addr = Output(UInt(addrBits.W))
}

class AXI4LiteBundleW extends AXI4Bundle with AXI4HasData {
    val strb = Output(UInt((dataBits/8).W))
}

class AXI4LiteBundleB extends AXI4Bundle {
    val resp = Output(UInt(respBits.W))
}

class AXI4LiteBundleR extends AXI4Bundle with AXI4HasData {
    val resp = Output(UInt(respBits.W))
}

class AXI4Lite extends Bundle {
    val aw = Decoupled(new AXI4LiteBundleA)
    val w  = Decoupled(new AXI4LiteBundleW)
    val b  = Flipped(Decoupled(new AXI4LiteBundleB))
    val ar = Decoupled(new AXI4LiteBundleA)
    val r  = Flipped(Decoupled(new AXI4LiteBundleR))
}

// AXI4-full

class AXI4BundleA extends AXI4LiteBundleA {
    val len  = Output(UInt(lenBits.W))
    val size = Output(UInt(sizeBits.W))
    val burst = Output(UInt(burstBits.W))
}

class AXI4BundleW extends AXI4LiteBundleW with AXI4HasLast

class AXI4BundleB extends AXI4LiteBundleB

class AXI4BundleR extends AXI4LiteBundleR with AXI4HasLast

class AXI4 extends AXI4Lite {
    override val aw = Decoupled(new AXI4BundleA)
    override val w  = Decoupled(new AXI4BundleW)
    override val b  = Flipped(Decoupled(new AXI4BundleB))
    override val ar = Decoupled(new AXI4BundleA)
    override val r  = Flipped(Decoupled(new AXI4BundleR))
}