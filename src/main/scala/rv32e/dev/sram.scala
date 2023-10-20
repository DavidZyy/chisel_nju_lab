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

class SRAM extends Module {
    val axi = IO(new AXILiteIO_slave)

    val s_idle :: s_delay :: s_busy :: Nil = Enum(3)
    val state = RegInit(s_idle)
    // state := MuxLookup(state, s_idle, List(
    //     s_idle      ->  Mux(axi.ar.valid, s_busy, s_idle), //set delay = 10
    //     s_busy      ->  Mux(axi.r.ready, s_idle, s_busy) //delay == 0, transe
    // ))
    axi.ar.ready := MuxLookup(state, false.B, List( s_idle  ->  true.B))
    axi.r.valid  := MuxLookup(state, false.B, List( s_busy  ->  true.B))

    val lfsr = Module(new LFSR())

    val delay = RegInit(0.U)
    // axi.r.valid  := false.B
    // axi.ar.ready := true.B
    switch (state) {
        is  (s_idle) {
            delay   :=  0.U
            when (axi.ar.valid) {
                state   :=  s_delay
                // delay   :=  lfsr.io.out
                // axi.r.valid  := true.B
                // axi.ar.ready := false.B
            } .otherwise {
                state   :=  s_idle
            }
        }
        is (s_delay) {
            when (delay === 0.U) {
                state := s_busy
            } .otherwise {
                delay := delay - 1.U
            }
        }
        is (s_busy) {
            when (axi.r.ready) {
                state   :=  s_idle
                // axi.r.valid  := false.B
                // axi.ar.ready := true.B
            } .otherwise {
                state   :=  s_busy
            }
        }
    }

    val RomBB_i1 = Module(new RomBB())

    RomBB_i1.io.addr := axi.ar.bits.addr
    axi.r.bits.data := RomBB_i1.io.inst
}
