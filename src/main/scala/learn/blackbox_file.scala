package learn

import chisel3._
import chisel3.util.HasBlackBoxResource

class BlackBoxRealAdd_file extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val in1 = Input(UInt(64.W))
    val in2 = Input(UInt(64.W))
    val out = Output(UInt(64.W))
  })
  addResource("/real_math.v")
}

class black_add_file extends Module {
    val io = IO(new Bundle {
        val in1 = Input(UInt(64.W))
        val in2 = Input(UInt(64.W))
        val out = Output(UInt(64.W))
    })

    val kan = Module(new BlackBoxRealAdd_file())    
    kan.io.in1  := io.in1
    kan.io.in2  := io.in2
    io.out  := kan.io.out
}

object black_file_main extends App {
    emitVerilog(new black_add_file(), Array("--target-dir", "generated/black_file"))
}
