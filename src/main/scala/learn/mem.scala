package learn

import chisel3._
import chisel3.util._
import chisel3.tester._
import chisel3.tester.RawTester.test

class ReadWriteSmem extends Module {
  val width: Int = 32
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val write = Input(Bool())
    val addr = Input(UInt(10.W))
    val dataIn = Input(UInt(width.W))
    val dataOut = Output(UInt(width.W))
  })

  val mem = SyncReadMem(1024, UInt(width.W))
  // Create one write port and one read port
  mem.write(io.addr, io.dataIn)
  io.dataOut := mem.read(io.addr, io.enable)
}

class WriteSmem extends Module {
  val width: Int = 32
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val write = Input(Bool())
    val addr = Input(UInt(10.W))
    val dataIn = Input(UInt(width.W))
    val dataOut = Output(UInt(width.W))
  })

  val mem = Mem(1024, UInt(width.W))
  // Create one write port and one read port
  mem.write(io.addr, io.dataIn)
//   io.dataOut := mem.read(io.addr, io.enable)
  io.dataOut := mem.read(io.addr)
}

object mem_main extends App {
    emitVerilog(new ReadWriteSmem(), Array("--target-dir", "generated"))
    emitVerilog(new WriteSmem(), Array("--target-dir", "generated"))
}
