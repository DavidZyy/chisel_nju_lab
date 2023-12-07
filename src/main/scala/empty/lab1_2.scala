/*
 * Dummy file to start a Chisel project.
 *
 * Author: Martin Schoeberl (martin@jopdesign.com)
 * 
 */

package empty

import chisel3._
import chisel3.util._

object Path {
  val path: String = "/home/zhuyangyang/project/nju_digital_design_chisel/"
}

/**
  * lab1
  *
  */
class multiplexer extends Module {
  val io = IO(new Bundle {
    val x0 = Input(UInt(2.W))
    val x1 = Input(UInt(2.W))
    val x2 = Input(UInt(2.W))
    val x3 = Input(UInt(2.W))
    val y  = Input(UInt(2.W))
    val f  = Output(UInt(2.W))
  })

  when(io.y === 0.U) {
    io.f := io.x0
  }.elsewhen(io.y === 1.U) {
    io.f := io.x1
  }.elsewhen(io.y === 2.U) {
    io.f := io.x2
  }.otherwise {
    io.f := io.x3
  }
}

object MultiplexerMain extends  App {
  // import Path.path
  val rela_path: String = "lab1/multiplexer/vsrc"
  println("Generating the multiplexer hardware")
  emitVerilog(new multiplexer(), Array("--target-dir", Path.path + rela_path))
}

/**
  * lab2
  *
  */
class Decoder24 extends Module {
  val io = IO(new Bundle {
    val x  = Input(UInt(2.W))
    val en = Input(Bool())
    val y  = Output(UInt(4.W))
  })

  when(io.en) {
    io.y := MuxLookup(io.x, 0.U)(Seq(
      0.U -> "b0001".U,
      1.U -> "b0010".U,
      2.U -> "b0100".U,
      3.U -> "b1000".U
    ))
  }.otherwise {
    io.y := 0.U
  }
}

object Decoder24Main extends App {

  val rela_path: String = "lab2/decoder24/vsrc"
  println("Generating the decoder24 hardware")
  emitVerilog(new Decoder24(), Array("--target-dir", Path.path + rela_path))
  // emitVerilog(new Decoder24(), Array("--target-dir", "/home/zhuyangyang/project/nju_digital_design_chisel/lab1/multiplexer/vsrc"))
}

class encoder24 extends Module {
  val io = IO(new Bundle {
    val x  = Input(UInt(4.W))
    val en = Input(Bool()) 
    val y  = Output(UInt(2.W))
  })

  when(io.en) {
    io.y := MuxLookup(io.x, 0.U)(Seq(
      "b0001".U -> 0.U,
      "b0010".U -> 1.U,
      "b0100".U -> 2.U,
      "b1000".U -> 3.U,
    ))
  }.otherwise {
    io.y := 0.U
  }
}

object Encoder24Main extends App {
  val rela_path: String = "lab2/encoder24/vsrc"
  println("Generating the encoder24 hardware")
  emitVerilog(new encoder24(), Array("--target-dir", Path.path + rela_path))
}


class encoder_prio83 extends Module {
  val io = IO(new Bundle {
    val x  = Input(UInt(8.W))
    val en = Input(Bool())
    val y  = Output(UInt(3.W))
  })

  io.y := "b000".U
  when(io.en) {
    for (i <- 0 to 7 by 1) {
      when(io.x(i) === 1.U) {
        io.y := i.U(3.W)
      }
    }
  }.otherwise {
    io.y := "b000".U
  }
}

object encoder83Main extends App {
  val rela_path: String = "lab2/encoder_prio83/vsrc"
  emitVerilog(new encoder_prio83(), Array("--target-dir", Path.path + rela_path))
}

