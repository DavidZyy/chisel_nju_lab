package learn

import chisel3._
import chisel3.util._

import _root_.circt.stage.ChiselStage
import _root_.circt.stage.CIRCTTargetAnnotation
import _root_.circt.stage.CIRCTTarget
import _root_.circt.stage.FirtoolOption

class TestOneHot extends Module {
  val io = IO(new Bundle {
    val selector = Input(UInt(4.W))
    val hotValue = Output(UInt(4.W))
  })

  io.hotValue := Mux1H(Seq(
    io.selector(0) -> 2.U,
    io.selector(1) -> 4.U,
    io.selector(2) -> 8.U,
    io.selector(3) -> 11.U
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

object myMultipierMain extends App {
  val path = "/home/zhuyangyang/project/CPU/chisel-empty/generated"
  val firtoolOptions = Seq (
      FirtoolOption("--disable-all-randomization"),
      FirtoolOption("--lowering-options=disallowLocalVariables, locationInfoStyle=none"),
      // FirtoolOption("--lowering-options=locationInfoStyle=none")
  )
  emitVerilog(new TestOneHot(), Array("--target-dir", path), firtoolOptions)
}

