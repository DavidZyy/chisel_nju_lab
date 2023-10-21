package rv32e.dev

// the codes here modified according to  
// https://ysyx.oscc.cc/slides/2205/18.html#/axi-lite%E6%8E%A5%E5%8F%A3%E7%9A%84rom
// A sram with axi-lite bus, it acts as a slave, and IFU is it's master.
// wrapper for DPI-C verilog, connected to IFU, LSU

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.define.Dec_Info._
import rv32e.define.Inst._
import rv32e.bus.AXILiteIO_slave
import rv32e.utils.LFSR

class RomBB extends BlackBox with HasBlackBoxResource {
    val io = IO(new Bundle {
        val addr = Input(UInt(ADDR_WIDTH.W))
        val inst = Output(UInt(ADDR_WIDTH.W))
    })

    addResource("/RomBB.v")
}

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

class SRAM extends Module {
    val axi = IO(new AXILiteIO_slave)

    val s_idle :: s_read_delay :: s_read_end :: s_write_delay :: s_write_end :: Nil = Enum(5)
    val state = RegInit(s_idle)

    axi.ar.ready := MuxLookup(state, false.B, List( s_idle      ->  true.B))
    axi.r.valid  := MuxLookup(state, false.B, List( s_read_end  ->  true.B))

    axi.aw.ready := MuxLookup(state, false.B, List( s_idle      ->  true.B))
    axi.w.ready  := MuxLookup(state, false.B, List( s_idle      ->  true.B))
    axi.b.valid  := MuxLookup(state, false.B, List( s_write_end  ->  true.B))

    val lfsr = Module(new LFSR())
    val delay = RegInit(0.U)

    switch (state) {
        is (s_idle) {
            delay := 0.U
            // delay := lfsr.io.out;
            when (axi.ar.fire) {
                state := s_read_delay
            } 
            .elsewhen (axi.aw.fire && axi.w.fire) {
                state := s_write_delay
            }
            .otherwise {
                state := s_idle
            }
        }
        is (s_read_delay) {
            state := Mux(delay === 0.U, s_read_end, s_read_delay)
            delay := delay - 1.U
        }
        is (s_read_end) {
            state := Mux(axi.r.fire, s_idle, s_read_end)
        }
        is (s_write_delay) {
            state := Mux(delay === 0.U, s_write_end, s_write_delay)
            delay := delay - 1.U
        }
        is (s_write_end) {
            state := Mux(axi.b.fire, s_idle, s_write_end)
        }
    }

    val RamBB_i1 = Module(new RamBB())

    RamBB_i1.io.clock   := clock
    RamBB_i1.io.addr    := MuxLookup(state, 0.U, List(
        s_read_end  -> axi.ar.bits.addr,
        s_write_end -> axi.aw.bits.addr,
    ))
    RamBB_i1.io.mem_wen := MuxLookup(state, false.B, List(
        s_write_end -> true.B
    ))
    RamBB_i1.io.valid   := MuxLookup(state, false.B, List(
        s_read_end  -> true.B,
        s_write_end -> true.B
    ))
    RamBB_i1.io.wdata   :=  0.U
    RamBB_i1.io.wmask   :=  0.U
    RamBB_i1.io.wdata   :=  axi.w.bits.data
    RamBB_i1.io.wmask   :=  axi.w.bits.strb

    axi.r.bits.data := RamBB_i1.io.rdata
}