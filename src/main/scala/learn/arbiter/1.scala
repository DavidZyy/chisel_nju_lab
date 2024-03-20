package learn.arbiter

import chisel3._
import chisel3.util._

import _root_.circt.stage.ChiselStage
import _root_.circt.stage.CIRCTTargetAnnotation
import _root_.circt.stage.CIRCTTarget
import _root_.circt.stage.FirtoolOption

class TestArbiter1 extends Module {
    val inNum = 3
    val dataWidth = 32
    val io = IO(new Bundle {
        val in  = Flipped(Vec(inNum, Decoupled(Input(UInt(dataWidth.W)))))
        val out = Decoupled(Output(UInt(dataWidth.W)))
    })

    val arb = Module(new Arbiter(UInt(dataWidth.W), inNum))

    arb.io.in <> io.in
    io.out <> arb.io.out
}

object TestArbiter1Main extends App {
  val path = "/home/zhuyangyang/project/CPU/chisel-empty/generated"
  val firtoolOptions = Seq (
      FirtoolOption("--disable-all-randomization"),
      FirtoolOption("--lowering-options=disallowLocalVariables, locationInfoStyle=none"),
      // FirtoolOption("--lowering-options=locationInfoStyle=none")
  )
  emitVerilog(new TestArbiter1(), Array("--target-dir", path), firtoolOptions)
}


class TestLockingArbiter1 extends Module {
    val inNum = 3
    val dataWidth = 32
    val cnt = 7
    val io = IO(new Bundle {
        val in  = Flipped(Vec(inNum, Decoupled(Input(UInt(dataWidth.W)))))
        val out = Decoupled(Output(UInt(dataWidth.W)))
    })

    val arb = Module(new LockingArbiter(UInt(dataWidth.W), inNum, cnt))

    arb.io.in <> io.in
    io.out <> arb.io.out
}

object TestLockingArbiter1  extends App {
  val path = "/home/zhuyangyang/project/CPU/chisel-empty/generated"
  val firtoolOptions = Seq (
      FirtoolOption("--disable-all-randomization"),
      FirtoolOption("--lowering-options=disallowLocalVariables, locationInfoStyle=none"),
      // FirtoolOption("--lowering-options=locationInfoStyle=none")
  )
  emitVerilog(new TestLockingArbiter1(), Array("--target-dir", path), firtoolOptions)
}