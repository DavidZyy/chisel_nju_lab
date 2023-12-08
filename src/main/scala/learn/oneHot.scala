package learn

import chisel3._
import chisel3.util._

class TestOneHot extends Module {
  val io = IO(new Bundle {
    val selector = Input(UInt(4.W))
    val hotValue = Output(UInt(4.W))
  })

  io.hotValue := Mux1H(Seq(
    io.selector(0) -> 1.U,
    io.selector(1) -> 2.U,
    io.selector(2) -> 3.U,
    io.selector(3) -> 4.U
  ))
}

class TestOneHot1 extends Module {
  val io = IO(new Bundle {
    val selector = Input(UInt(4.W))
    val hotValue = Output(UInt(4.W))
  })

//   val a = Vec(4, 1.W)
  val a = List(1.U, 2.U, 3.U, 4.U)
  io.hotValue := Mux1H(io.selector, a)
}

object TestOneHotmain extends App {
    // emitVerilog(new TestOneHot(),  Array("--target-dir", "generated"))
    // emitVerilog(new TestOneHot1(), Array("--target-dir", "generated"))
    // emitVerilog(new WriteSmem(),   Array("--target-dir", "generated"))
    val out = Vec(4, UInt(2.W))

    // println(out.map())
}
