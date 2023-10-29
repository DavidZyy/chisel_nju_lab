package learn.adder

import chisel3._
import chisel3.util._

class FullAdder extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(1.W))
    val b = Input(UInt(1.W))
    val cin = Input(UInt(1.W))
    val sum = Output(UInt(1.W))
    val cout = Output(UInt(1.W))
  })

  val sum_ab = io.a ^ io.b
  val sum_abc = sum_ab ^ io.cin
  val cout_ab = (io.a & io.b) | (sum_ab & io.cin)

  io.sum := sum_abc
  io.cout := cout_ab
}
