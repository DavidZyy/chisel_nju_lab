/**
  * Intra-block parallel, inter-block parallel adder
  */
package learn.adder

import chisel3._
import chisel3.util._
import Constants._

class CarryLookaheadAdderBlock2 extends Module {
  val io = IO(new Bundle {
    val p    = Input(UInt(BLK_WDT.W))
    val g    = Input(UInt(BLK_WDT.W))
    val cin  = Input(UInt(1.W)) //c0
    val c    = Output(UInt(BLK_WDT.W))
    val P    = Output(UInt(1.W))
    val G    = Output(UInt(1.W))
  })

  val c = Wire(Vec(BLK_WDT, UInt(1.W)))
  val p = io.p
  val g = io.g

  c(0) := io.cin
  c(1) := g(0) | (p(0) & c(0))
  c(2) := g(1) | (p(1) & g(0)) | (p(1) & p(0) & c(0))
  c(3) := g(2) | (p(2) & g(1)) | (p(2) & p(1) & g(0)) | (p(2) & p(1) & p(0) & c(0))

  io.P := p(3) & p(2) & p(1) & p(0)
  io.G := g(3) | (p(3) & g(2)) | (p(3) & p(2) & g(1)) | (p(3) & p(2) & p(1) & g(0))
  io.c := c.asUInt
}

class adder_v2 extends Module {
  val blk_num_l1 = 16
  val blk_num_l2 = 4
  val io = IO(new Bundle {
    val A    = Input(UInt(ADR_WDT.W))
    val B    = Input(UInt(ADR_WDT.W))
    val Cin  = Input(UInt(1.W))
    val Sum  = Output(UInt(ADR_WDT.W))
    val Cout = Output(UInt(1.W))
  })


  val blocks_l1 = Seq.fill(blk_num_l1)(Module(new CarryLookaheadAdderBlock2()))
  val blocks_l2 = Seq.fill(blk_num_l2)(Module(new CarryLookaheadAdderBlock2()))
  val blocks_l3 = Module(new CarryLookaheadAdderBlock2())

  val p = io.A | io.B
  val g = io.A & io.B
  val c = Wire(Vec(blk_num_l1, UInt(BLK_WDT.W)))

  for (i <- 0 until blk_num_l1) {
    blocks_l1(i).io.p := p((i+1)*BLK_WDT-1, i*BLK_WDT)
    blocks_l1(i).io.g := g((i+1)*BLK_WDT-1, i*BLK_WDT)
    blocks_l1(i).io.cin := blocks_l2(i/4).io.c(i%4)
    c(i) := blocks_l1(i).io.c
  }

  for (i <- 0 until blk_num_l2) {
    blocks_l2(i).io.p := Cat(blocks_l1(i*4+3).io.P, blocks_l1(i*4+2).io.P, blocks_l1(i*4+1).io.P, blocks_l1(i*4).io.P)
    blocks_l2(i).io.g := Cat(blocks_l1(i*4+3).io.G, blocks_l1(i*4+2).io.G, blocks_l1(i*4+1).io.G, blocks_l1(i*4).io.G)
    blocks_l2(i).io.cin := blocks_l3.io.c(i)
  }

  blocks_l3.io.cin := io.Cin //c0
  blocks_l3.io.p := Reverse(Cat(blocks_l2.map(_.io.P)))
  blocks_l3.io.g := Reverse(Cat(blocks_l2.map(_.io.G)))

  // Create an array of 64 full adders
  val fullAdders = VecInit(Seq.fill(ADR_WDT)(Module(new FullAdder()).io))

  val cc = c.asUInt()
  for (i <- 0 until ADR_WDT) {
    fullAdders(i).a := io.A(i)
    fullAdders(i).b := io.B(i)
    fullAdders(i).cin := cc(i)
  }

  io.Sum := Reverse(Cat(fullAdders.map(_.sum))).asUInt()
  io.Cout := blocks_l3.io.G | blocks_l3.io.P & blocks_l3.io.cin
}

object adder_v2 extends App {
  emitVerilog(new adder_v2(), Array("--target-dir", "generated"))
}

