package learn.adder

import chisel3._
import chisel3.util._
import Constants._

// only use fulladder to construct this adder
class SerialCarryAdder extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(ADR_WDT.W))
    val b = Input(UInt(ADR_WDT.W))
    val cin = Input(UInt(1.W))
    val sum = Output(UInt(ADR_WDT.W))
    val cout = Output(UInt(1.W))
  })

  // Create an array of 64 full adders
  val fullAdders = VecInit(Seq.fill(ADR_WDT)(Module(new FullAdder()).io))

  // Connect the inputs to the first full adder
  fullAdders(0).a := io.a(0)
  fullAdders(0).b := io.b(0)
  fullAdders(0).cin := io.cin

  // Connect the outputs of the full adders in a serial chain
  for (i <- 1 until ADR_WDT) {
    fullAdders(i).a := io.a(i)
    fullAdders(i).b := io.b(i)
    fullAdders(i).cin := fullAdders(i - 1).cout
  }

  // Connect the outputs of the last full adder to the outputs of the module
  io.sum := Reverse(Cat(fullAdders.map(_.sum))).asUInt()
  io.cout := fullAdders(ADR_WDT-1).cout
}

object SerialCarryAdder extends App {
  emitVerilog(new SerialCarryAdder(), Array("--target-dir", "generated"))
}
