package rv32e.fu

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.define.Dec_Info._
import rv32e.define.Inst._
import rv32e.bus.AXILiteIO_master_lsu

class ram_in_class extends Bundle {
    val valid   =   Input(Bool())
    val mem_wen =   Input(Bool())
    val addr    =   Input(UInt(ADDR_WIDTH.W))
    val wdata   =   Input(UInt(DATA_WIDTH.W))
    val op      =   Input(UInt(LSUOP_WIDTH.W))
}

class ram_out_class extends Bundle {
    val rdata   =   Output(UInt(DATA_WIDTH.W))
    val end     =   Output(Bool())
    val idle    =   Output(Bool())
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

class Lsu extends Module {
    val io = IO(new Bundle {
        val in  = (new ram_in_class )
        val out = (new ram_out_class)
    })
    val axi = IO(new AXILiteIO_master_lsu)

    val s_idle :: s_read_request :: s_read_wait :: s_write_request :: s_write_wait :: s_end :: Nil = Enum(6)
    val state = RegInit(s_idle)

    switch (state) {
        is (s_idle) {
            when (io.in.valid) {
                when (io.in.mem_wen) {
                    state := s_write_request
                } .otherwise {
                    state := s_read_request
                }
            } .otherwise {
                state := s_idle
            }
        }
        is (s_read_request) {
            state := s_read_wait
        }
        is (s_read_wait) {
            state := Mux(axi.r.fire, s_end, s_read_wait)
            // state := s_end
        }
        is (s_write_request) {
            state := s_write_wait
        }
        is (s_write_wait) {
            state := Mux(axi.b.fire, s_end, s_write_wait)
        }
        is (s_end) {
            state := s_idle
        }
    }

    io.out.idle  := MuxLookup(state, false.B, List(s_idle -> true.B))
    io.out.end   := MuxLookup(state, false.B, List(s_end  -> true.B))

    axi.ar.valid := MuxLookup(state, false.B, List(s_read_request  -> true.B))
    axi.r.ready  := MuxLookup(state, false.B, List(s_read_wait     -> true.B))

    axi.aw.valid := MuxLookup(state, false.B, List(s_write_request -> true.B))
    axi.w.valid  := MuxLookup(state, false.B, List(s_write_request -> true.B))
    axi.b.ready  := MuxLookup(state, false.B, List(s_write_wait    -> true.B))

    axi.ar.bits.addr := io.in.addr
    axi.aw.bits.addr := io.in.addr
    axi.w.bits.data  := io.in.wdata

    val lsu_op = io.in.op
    val true_addr = io.in.addr

    val addr_low_2 = true_addr(1, 0)

    val valid = io.in.valid

//     val RamBB_i1 = Module(new RamBB())
// 
//     RamBB_i1.io.clock   := clock
//     RamBB_i1.io.addr    := io.in.addr
//     RamBB_i1.io.mem_wen := io.in.mem_wen
//     RamBB_i1.io.valid   := valid

    val rdata_align_4 = Wire(UInt(DATA_WIDTH.W))
    rdata_align_4 := axi.r.bits.data


    val lb_rdata  = Wire(UInt(DATA_WIDTH.W))
    val lbu_rdata = Wire(UInt(DATA_WIDTH.W))
    val lh_rdata  = Wire(UInt(DATA_WIDTH.W))
    val lhu_rdata = Wire(UInt(DATA_WIDTH.W))
    val lw_rdata  = Wire(UInt(DATA_WIDTH.W))

    lb_rdata := MuxLookup(addr_low_2, 0.U, Array(
        0.U -> Cat(Fill(24, rdata_align_4(7 )), rdata_align_4(7, 0)),
        1.U -> Cat(Fill(24, rdata_align_4(15)), rdata_align_4(15, 8)),
        2.U -> Cat(Fill(24, rdata_align_4(23)), rdata_align_4(23, 16)),
        3.U -> Cat(Fill(24, rdata_align_4(31)), rdata_align_4(31, 24)),
    ))

    lbu_rdata := MuxLookup(addr_low_2, 0.U, Array(
        0.U -> Cat(Fill(24, 0.U), rdata_align_4(7, 0)),
        1.U -> Cat(Fill(24, 0.U), rdata_align_4(15, 8)),
        2.U -> Cat(Fill(24, 0.U), rdata_align_4(23, 16)),
        3.U -> Cat(Fill(24, 0.U), rdata_align_4(31, 24)),
    ))

    lh_rdata := MuxLookup(addr_low_2, 0.U, Array(
        0.U -> Cat(Fill(16, rdata_align_4(15)), rdata_align_4(15, 0 )),
        2.U -> Cat(Fill(16, rdata_align_4(31)), rdata_align_4(31, 16)),
    ))

    lhu_rdata := MuxLookup(addr_low_2, 0.U, Array(
        0.U -> Cat(Fill(16, 0.U), rdata_align_4(15, 0 )),
        2.U -> Cat(Fill(16, 0.U), rdata_align_4(31, 16)),
    ))

    lw_rdata := rdata_align_4

    io.out.rdata    :=  MuxLookup(lsu_op, 0.U, Array(
        ("b" + lsu_x  ).U -> 0.U,
        ("b" + lsu_lb ).U -> lb_rdata,
        ("b" + lsu_lbu).U -> lbu_rdata,
        ("b" + lsu_lh ).U -> lh_rdata,
        ("b" + lsu_lhu).U -> lhu_rdata,
        ("b" + lsu_lw ).U -> lw_rdata,
    ))

    val sb_wmask = Wire(UInt((DATA_WIDTH/BYTE_WIDTH).W))
    val sh_wmask = Wire(UInt((DATA_WIDTH/BYTE_WIDTH).W))
    val sw_wmask = Wire(UInt((DATA_WIDTH/BYTE_WIDTH).W))

    sb_wmask    :=  MuxLookup(addr_low_2, 0.U, Array(
        0.U -> "b0001".U,
        1.U -> "b0010".U,
        2.U -> "b0100".U,
        3.U -> "b1000".U,
    ))

    sh_wmask    :=  MuxLookup(addr_low_2, 0.U, Array(
        0.U -> "b0011".U,
        2.U -> "b1100".U,
    ))

    sw_wmask    :=  "b1111".U

    val wmask   =  MuxLookup(lsu_op, 0.U, Array(
        ("b" + lsu_sb).U -> sb_wmask,
        ("b" + lsu_sh).U -> sh_wmask,
        ("b" + lsu_sw).U -> sw_wmask,
    ))

    axi.w.bits.strb  := wmask

    // RamBB_i1.io.wdata   :=  io.in.wdata
    // RamBB_i1.io.wmask   :=  wmask 
}


// object decoder_main extends App {
//     emitVerilog(new Ram(), Array("--target-dir", "generated"))
//     // emitVerilog(new WriteSmem(), Array("--target-dir", "generated"))
// }