package rv32e.device

import chisel3._
import chisel3.util._

import rv32e.core.config._

class RamBB extends BlackBox with HasBlackBoxResource {
    val io = IO(new Bundle {
        val clock   = Input(Clock())
        val addr    = Input(UInt(DATA_WIDTH.W))
        val mem_wen = Input(Bool())
        val valid   = Input(Bool())
        val wdata   = Input(UInt(DATA_WIDTH.W))
        val wmask   = Input(UInt((DATA_WIDTH/BYTE_WIDTH).W))
        val rdata   = Output(UInt(DATA_WIDTH.W))
    })
    addResource("/RamBB.v")
}
