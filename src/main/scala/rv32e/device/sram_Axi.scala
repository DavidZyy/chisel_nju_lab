/**
  * a sram module use dpi-c as axi slave
  * refer https://zipcpu.com/blog/2019/05/29/demoaxi.html
  */
package rv32e.device

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.config.Axi_Configs._
import rv32e.define.Dec_Info._
import rv32e.define.Inst._
import rv32e.bus._
import rv32e.utils.LFSR

class SRAM_axi extends Module {
    val axi = IO(Flipped(new AXIIO))

    val lfsr = Module(new LFSR())
    val delay = RegInit(0.U)

    // store the request, until next request
    val reg_AxLen = RegInit(0.U)
    val reg_addr  = RegInit(0.U)
    val reg_burst = RegInit(3.U)

    // state_sram machine
    val s_idle :: s_read_delay :: s_read_mid :: s_read_end :: Nil = Enum(4)
    val state_sram = RegInit(s_idle)
    switch (state_sram) {
        is (s_idle) {
            delay := 0.U
            // delay := lfsr.io.out;
            when (axi.ar.fire) {
                state_sram := s_read_delay
                reg_AxLen  := axi.ar.bits.len
                reg_addr   := axi.ar.bits.addr
                reg_burst  := axi.ar.bits.burst
            } .otherwise {
                state_sram := s_idle
            }
        }
        is (s_read_delay) {
            when (delay === 0.U) {
                state_sram := Mux(reg_AxLen === 0.U, s_read_end, s_read_mid)
            } otherwise {
                state_sram := s_read_delay
            }
            delay := delay - 1.U
        }
        is (s_read_mid) {
            state_sram  := Mux(reg_AxLen === 1.U, s_read_end, s_read_mid)
            reg_AxLen   := Mux(axi.r.fire, reg_AxLen-1.U, reg_AxLen)
            reg_addr    := MuxLookup(reg_burst, reg_addr)(List(
                INCR.U -> Mux(axi.r.fire, (reg_addr + ADDR_BYTE.U), reg_addr)
            ))
        }
        is (s_read_end) {
            state_sram := s_idle
        }
    }

    val RamBB_i1 = Module(new RamBB())
    RamBB_i1.io.clock   := clock
    RamBB_i1.io.addr    := reg_addr
    RamBB_i1.io.mem_wen := MuxLookup(state_sram, false.B)(List())
    RamBB_i1.io.valid   := MuxLookup(state_sram, false.B)(List( s_read_mid  -> true.B, s_read_end  -> true.B))
    RamBB_i1.io.wdata   :=  axi.w.bits.data
    RamBB_i1.io.wmask   :=  axi.w.bits.strb

    // axi slave signals
    axi.ar.ready    := MuxLookup(state_sram, false.B)(List( s_idle      ->  true.B))
    axi.r.valid     := MuxLookup(state_sram, false.B)(List( s_read_mid  ->  true.B, s_read_end -> true.B))
    axi.r.bits.data := RamBB_i1.io.rdata
    axi.r.bits.resp := 0.U
    axi.r.bits.last := Mux(state_sram === s_read_end, true.B, false.B)
    axi.aw.ready    := MuxLookup(state_sram, false.B)(List())
    axi.w.ready     := MuxLookup(state_sram, false.B)(List())
    axi.b.valid     := MuxLookup(state_sram, false.B)(List())
    axi.b.bits.resp := 0.U
}

class sram_axi_rw extends Module {
    val axi = IO(Flipped(new AXIIO))

    val lfsr = Module(new LFSR())
    val delay = RegInit(0.U)

    // store the request, until next request
    val reg_AxLen = RegInit(0.U)
    val reg_addr  = RegInit(0.U)
    val reg_burst = RegInit(3.U)

    // state machine s_fill : sram wait a cycle to wait dcahe fill pipeline, for it read data need two cycles
    //  0         1               2             3             4                5         6              7
    val s_idle :: s_read_delay :: s_read_mid :: s_read_end :: s_write_delay :: s_fill :: s_write_mid :: s_write_end :: Nil = Enum(8)
    val state_sram = RegInit(s_idle)
    switch (state_sram) {
        is (s_idle) {
            delay := 0.U
            // delay := lfsr.io.out;
            when (axi.ar.fire) {
                state_sram := s_read_delay
                reg_AxLen  := axi.ar.bits.len
                reg_addr   := axi.ar.bits.addr
                reg_burst  := axi.ar.bits.burst
            } .elsewhen (axi.aw.fire) {
                state_sram := s_write_delay
                reg_AxLen  := axi.aw.bits.len
                reg_addr   := axi.aw.bits.addr
                reg_burst  := axi.aw.bits.burst
            } .otherwise {
                state_sram := s_idle
            }
        }
        is (s_read_delay) {
            when (delay === 0.U) {
                state_sram := Mux(reg_AxLen === 0.U, s_read_end, s_read_mid)
            } otherwise {
                state_sram := s_read_delay
            }
            delay := delay - 1.U
        }
        is (s_read_mid) {
            state_sram  := Mux(reg_AxLen === 1.U, s_read_end, s_read_mid)
            reg_AxLen   := Mux(axi.r.fire, reg_AxLen-1.U, reg_AxLen)
            reg_addr    := MuxLookup(reg_burst, reg_addr)(List(
                INCR.U -> Mux(axi.r.fire, (reg_addr + ADDR_BYTE.U), reg_addr)
            ))
        }
        is (s_read_end) {
            state_sram := s_idle
        }
        is (s_write_delay) {
            when (delay === 0.U) {
                // state_sram := Mux(reg_AxLen === 0.U, s_write_end, s_write_mid)
                state_sram := s_fill
            } otherwise {
                state_sram := s_write_delay
            }
            delay := delay - 1.U
        }
        is (s_fill) {
            state_sram  := Mux(reg_AxLen === 0.U, s_write_end, s_write_mid)
        }
        is (s_write_mid) {
            state_sram  := Mux(reg_AxLen === 1.U, s_write_end, s_write_mid)
            // state_sram  := Mux(axi.w.bits.last, s_write_end, s_write_mid)
            reg_AxLen   := Mux(axi.w.fire, reg_AxLen-1.U, reg_AxLen)
            reg_addr    := MuxLookup(reg_burst, reg_addr)(List(
                INCR.U -> Mux(axi.w.fire, (reg_addr + ADDR_BYTE.U), reg_addr)
            ))
        }
        is (s_write_end) {
            state_sram := s_idle
        }
    }

    val RamBB_i1 = Module(new RamBB())
    RamBB_i1.io.clock   := clock
    RamBB_i1.io.addr    := reg_addr
    RamBB_i1.io.mem_wen := MuxLookup(state_sram, false.B)(List(
        s_write_mid -> true.B, 
        s_write_end -> true.B,
    ))
    RamBB_i1.io.valid   := MuxLookup(state_sram, false.B)(List( 
        s_read_mid  -> true.B, 
        s_read_end  -> true.B,
        s_write_mid -> true.B, 
        s_write_end -> true.B,
    ))
    RamBB_i1.io.wdata   := axi.w.bits.data
    RamBB_i1.io.wmask   := axi.w.bits.strb

    // axi slave signals
    axi.ar.ready    := MuxLookup(state_sram, false.B)(List( s_idle  ->  true.B))
    axi.r.valid     := MuxLookup(state_sram, false.B)(List( s_read_mid  ->  true.B, s_read_end -> true.B))
    axi.r.bits.data := RamBB_i1.io.rdata
    axi.r.bits.resp := 0.U
    axi.r.bits.last := Mux(state_sram === s_read_end, true.B, false.B)
    axi.r.bits.resp := 0.U
    axi.aw.ready    := MuxLookup(state_sram, false.B)(List( s_idle  ->  true.B))
    axi.w.ready     := MuxLookup(state_sram, false.B)(List( s_fill -> true.B, s_write_mid -> true.B, s_write_end -> true.B))
    axi.b.valid     := MuxLookup(state_sram, false.B)(List( s_write_end -> true.B))
    axi.b.bits.resp := 0.U
}
