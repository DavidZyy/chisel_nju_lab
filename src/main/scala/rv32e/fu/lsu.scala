package rv32e.fu

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.define.Dec_Info._
import rv32e.define.Inst._
import rv32e.config.Axi_Configs._
import rv32e.bus._

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

class Lsu extends Module {
    val io = IO(new Bundle {
        val in  = (new ram_in_class )
        val out = (new ram_out_class)
    })
    val axi = IO(new AXILiteIO_master)

    // state_lsu machine
    val s_idle :: s_read_request :: s_read_wait :: s_write_request :: s_write_wait :: s_end :: Nil = Enum(6)
    val state_lsu = RegInit(s_idle)
    switch (state_lsu) {
        is (s_idle) {
            when (io.in.valid) {
                when (io.in.mem_wen) {
                    state_lsu := s_write_request
                } .otherwise {
                    state_lsu := s_read_request
                }
            } .otherwise {
                state_lsu := s_idle
            }
        }
        is (s_read_request) {
            state_lsu := Mux(axi.ar.fire, s_read_wait, s_read_request)
        }
        is (s_read_wait) {
            state_lsu := Mux(axi.r.fire, s_end, s_read_wait)
        }
        is (s_write_request) {
            state_lsu := Mux(axi.aw.fire && axi.w.fire, s_write_wait, s_write_request)
        }
        is (s_write_wait) {
            state_lsu := Mux(axi.b.fire, s_end, s_write_wait)
        }
        is (s_end) {
            state_lsu := s_idle
        }
    }

    val lsu_op = io.in.op
    val true_addr = io.in.addr

    val addr_low_2 = true_addr(1, 0)

    val valid = io.in.valid

    val rdata_align_4 = Wire(UInt(DATA_WIDTH.W))
    rdata_align_4 := axi.r.bits.data

    val lb_rdata  = Wire(UInt(DATA_WIDTH.W))
    val lbu_rdata = Wire(UInt(DATA_WIDTH.W))
    val lh_rdata  = Wire(UInt(DATA_WIDTH.W))
    val lhu_rdata = Wire(UInt(DATA_WIDTH.W))
    val lw_rdata  = Wire(UInt(DATA_WIDTH.W))

    lb_rdata := MuxLookup(addr_low_2, 0.U)(List(
        0.U -> Cat(Fill(24, rdata_align_4(7 )), rdata_align_4(7, 0)),
        1.U -> Cat(Fill(24, rdata_align_4(15)), rdata_align_4(15, 8)),
        2.U -> Cat(Fill(24, rdata_align_4(23)), rdata_align_4(23, 16)),
        3.U -> Cat(Fill(24, rdata_align_4(31)), rdata_align_4(31, 24)),
    ))

    lbu_rdata := MuxLookup(addr_low_2, 0.U)(List(
        0.U -> Cat(Fill(24, 0.U), rdata_align_4(7, 0)),
        1.U -> Cat(Fill(24, 0.U), rdata_align_4(15, 8)),
        2.U -> Cat(Fill(24, 0.U), rdata_align_4(23, 16)),
        3.U -> Cat(Fill(24, 0.U), rdata_align_4(31, 24)),
    ))

    lh_rdata := MuxLookup(addr_low_2, 0.U)(List(
        0.U -> Cat(Fill(16, rdata_align_4(15)), rdata_align_4(15, 0 )),
        2.U -> Cat(Fill(16, rdata_align_4(31)), rdata_align_4(31, 16)),
    ))

    lhu_rdata := MuxLookup(addr_low_2, 0.U)(List(
        0.U -> Cat(Fill(16, 0.U), rdata_align_4(15, 0 )),
        2.U -> Cat(Fill(16, 0.U), rdata_align_4(31, 16)),
    ))

    lw_rdata := rdata_align_4

    // store inst
    val sb_wmask = Wire(UInt((DATA_WIDTH/BYTE_WIDTH).W))
    val sh_wmask = Wire(UInt((DATA_WIDTH/BYTE_WIDTH).W))
    val sw_wmask = Wire(UInt((DATA_WIDTH/BYTE_WIDTH).W))

    sb_wmask    :=  MuxLookup(addr_low_2, 0.U)(List(
        0.U -> "b0001".U,
        1.U -> "b0010".U,
        2.U -> "b0100".U,
        3.U -> "b1000".U,
    ))

    sh_wmask    :=  MuxLookup(addr_low_2, 0.U)(List(
        0.U -> "b0011".U,
        2.U -> "b1100".U,
    ))

    sw_wmask    :=  "b1111".U

    val wmask   =  MuxLookup(lsu_op, 0.U)(List(
        ("b" + lsu_sb).U -> sb_wmask,
        ("b" + lsu_sh).U -> sh_wmask,
        ("b" + lsu_sw).U -> sw_wmask,
    ))

    io.out.idle  := MuxLookup(state_lsu, false.B)(List(s_idle -> true.B))
    io.out.end   := MuxLookup(state_lsu, false.B)(List(s_end  -> true.B))
    io.out.rdata    :=  MuxLookup(lsu_op, 0.U)(List(
        ("b" + lsu_x  ).U -> 0.U,
        ("b" + lsu_lb ).U -> lb_rdata,
        ("b" + lsu_lbu).U -> lbu_rdata,
        ("b" + lsu_lh ).U -> lh_rdata,
        ("b" + lsu_lhu).U -> lhu_rdata,
        ("b" + lsu_lw ).U -> lw_rdata,
    ))

    // axi master signals
    axi.ar.valid     := MuxLookup(state_lsu, false.B)(List(s_read_request  -> true.B))
    axi.ar.bits.addr := io.in.addr
    axi.r.ready      := MuxLookup(state_lsu, false.B)(List(s_read_wait     -> true.B))
    axi.aw.valid     := MuxLookup(state_lsu, false.B)(List(s_write_request -> true.B))
    axi.aw.bits.addr := io.in.addr
    axi.w.valid      := MuxLookup(state_lsu, false.B)(List(s_write_request -> true.B))
    axi.w.bits.data  := io.in.wdata
    axi.w.bits.strb  := wmask
    axi.b.ready      := MuxLookup(state_lsu, false.B)(List(s_write_wait    -> true.B))
}

// lsu with axi interface
class Lsu_axi extends Module {
    val io = IO(new Bundle {
        val in  = (new ram_in_class )
        val out = (new ram_out_class)
    })
    val axi = IO(new AXI4)
    // to_mem <> axi
    // val axi = new AXI4

    // state_lsu machine, issue aw signals in write_request, issue w signals in write_wait
    val s_idle :: s_read_request :: s_read_wait :: s_write_request :: s_write_wait :: s_end :: Nil = Enum(6)
    val state_lsu = RegInit(s_idle)
    switch (state_lsu) {
        is (s_idle) {
            when (io.in.valid) {
                when (io.in.mem_wen) {
                    state_lsu := s_write_request
                } .otherwise {
                    state_lsu := s_read_request
                }
            } .otherwise {
                state_lsu := s_idle
            }
        }
        is (s_read_request) {
            state_lsu := Mux(axi.ar.fire, s_read_wait, s_read_request)
        }
        is (s_read_wait) {
            state_lsu := Mux(axi.r.fire, s_end, s_read_wait)
        }
        is (s_write_request) {
            // maybe write should not wait? it issue request, and then do other ting.
            // this could be faster.
            state_lsu := Mux(axi.aw.fire, s_write_wait, s_write_request)
        }
        is (s_write_wait) {
            state_lsu := Mux(axi.b.fire, s_end, s_write_wait)
        }
        is (s_end) {
            state_lsu := s_idle
        }
    }

    val lsu_op = io.in.op
    val true_addr = io.in.addr

    val addr_low_2 = true_addr(1, 0)

    val valid = io.in.valid

    val rdata_align_4 = Wire(UInt(DATA_WIDTH.W))
    rdata_align_4 := axi.r.bits.data

    val lb_rdata  = Wire(UInt(DATA_WIDTH.W))
    val lbu_rdata = Wire(UInt(DATA_WIDTH.W))
    val lh_rdata  = Wire(UInt(DATA_WIDTH.W))
    val lhu_rdata = Wire(UInt(DATA_WIDTH.W))
    val lw_rdata  = Wire(UInt(DATA_WIDTH.W))

    lb_rdata := MuxLookup(addr_low_2, 0.U)(List(
        0.U -> Cat(Fill(24, rdata_align_4(7 )), rdata_align_4(7, 0)),
        1.U -> Cat(Fill(24, rdata_align_4(15)), rdata_align_4(15, 8)),
        2.U -> Cat(Fill(24, rdata_align_4(23)), rdata_align_4(23, 16)),
        3.U -> Cat(Fill(24, rdata_align_4(31)), rdata_align_4(31, 24)),
    ))

    lbu_rdata := MuxLookup(addr_low_2, 0.U)(List(
        0.U -> Cat(Fill(24, 0.U), rdata_align_4(7, 0)),
        1.U -> Cat(Fill(24, 0.U), rdata_align_4(15, 8)),
        2.U -> Cat(Fill(24, 0.U), rdata_align_4(23, 16)),
        3.U -> Cat(Fill(24, 0.U), rdata_align_4(31, 24)),
    ))

    lh_rdata := MuxLookup(addr_low_2, 0.U)(List(
        0.U -> Cat(Fill(16, rdata_align_4(15)), rdata_align_4(15, 0 )),
        2.U -> Cat(Fill(16, rdata_align_4(31)), rdata_align_4(31, 16)),
    ))

    lhu_rdata := MuxLookup(addr_low_2, 0.U)(List(
        0.U -> Cat(Fill(16, 0.U), rdata_align_4(15, 0 )),
        2.U -> Cat(Fill(16, 0.U), rdata_align_4(31, 16)),
    ))

    lw_rdata := rdata_align_4

    // store inst
    val sb_wmask = Wire(UInt((DATA_WIDTH/BYTE_WIDTH).W))
    val sh_wmask = Wire(UInt((DATA_WIDTH/BYTE_WIDTH).W))
    val sw_wmask = Wire(UInt((DATA_WIDTH/BYTE_WIDTH).W))

    sb_wmask    :=  MuxLookup(addr_low_2, 0.U)(List(
        0.U -> "b0001".U,
        1.U -> "b0010".U,
        2.U -> "b0100".U,
        3.U -> "b1000".U,
    ))

    sh_wmask    :=  MuxLookup(addr_low_2, 0.U)(List(
        0.U -> "b0011".U,
        2.U -> "b1100".U,
    ))

    sw_wmask    :=  "b1111".U

    val wmask   =  MuxLookup(lsu_op, 0.U)(List(
        ("b" + lsu_sb).U -> sb_wmask,
        ("b" + lsu_sh).U -> sh_wmask,
        ("b" + lsu_sw).U -> sw_wmask,
    ))

    io.out.idle  := MuxLookup(state_lsu, false.B)(List(s_idle -> true.B))
    io.out.end   := MuxLookup(state_lsu, false.B)(List(s_end  -> true.B))
    io.out.rdata    :=  MuxLookup(lsu_op, 0.U)(List(
        ("b" + lsu_x  ).U -> 0.U,
        ("b" + lsu_lb ).U -> lb_rdata,
        ("b" + lsu_lbu).U -> lbu_rdata,
        ("b" + lsu_lh ).U -> lh_rdata,
        ("b" + lsu_lhu).U -> lhu_rdata,
        ("b" + lsu_lw ).U -> lw_rdata,
    ))

    // axi master signals
    axi.ar.valid      := MuxLookup(state_lsu, false.B)(List(s_read_request  -> true.B))
    axi.ar.bits.addr  := io.in.addr
    axi.ar.bits.size  := MuxLookup(state_lsu , 0.U)(List(s_read_request -> DATA_WIDTH.U))
    axi.ar.bits.len   := 0.U
    axi.ar.bits.burst := INCR.U
    axi.r.ready       := MuxLookup(state_lsu, false.B)(List(s_read_wait     -> true.B))
    axi.aw.valid      := MuxLookup(state_lsu, false.B)(List(s_write_request -> true.B))
    axi.aw.bits.addr  := io.in.addr
    axi.aw.bits.size  := 0.U
    axi.aw.bits.len   := 0.U
    axi.aw.bits.burst := 0.U
    axi.w.valid       := MuxLookup(state_lsu, false.B)(List(s_write_wait -> true.B))
    axi.w.bits.data   := io.in.wdata
    axi.w.bits.strb   := wmask
    axi.w.bits.last   := true.B
    axi.b.ready       := MuxLookup(state_lsu, false.B)(List(s_write_wait -> true.B))
}

// lsu connects to dcache
class Lsu_cache extends Module {
    val io = IO(new Bundle {
        val in  = (new ram_in_class )
        val out = (new ram_out_class)
    })
    val to_cache   = IO(Decoupled(new LSU2Cache_bus))
    val from_cache = IO(Flipped(Decoupled(new Cache2LSU_bus)))

    // state_lsu machine, issue aw signals in write_request, issue w signals in write_wait
    val s_idle :: s_read_request :: s_read_wait :: s_write_request :: s_write_wait :: s_end :: Nil = Enum(6)
    val state_lsu = RegInit(s_idle)
    switch (state_lsu) {
        is (s_idle) {
            when (io.in.valid) {
                when (io.in.mem_wen) {
                    state_lsu := s_write_request
                } .otherwise {
                    state_lsu := s_read_request
                }
            } .otherwise {
                state_lsu := s_idle
            }
        }
        is (s_read_request) {
            state_lsu := Mux(to_cache.fire, s_read_wait, s_read_request)
        }
        is (s_read_wait) {
            state_lsu := Mux(from_cache.fire, s_end, s_read_wait)
        }
        is (s_write_request) {
            // maybe write should not wait? it issue request, and then do other ting.
            // this could be faster.
            state_lsu := Mux(to_cache.fire, s_write_wait, s_write_request)
        }
        is (s_write_wait) {
            state_lsu := Mux(from_cache.fire && from_cache.bits.bresp, s_end, s_write_wait)
            // state_lsu := s_end
        }
        is (s_end) {
            state_lsu := s_idle
        }
    }

    val lsu_op = io.in.op
    val true_addr = io.in.addr

    val addr_low_2 = true_addr(1, 0)

    val valid = io.in.valid

    val rdata_align_4 = Wire(UInt(DATA_WIDTH.W))
    rdata_align_4 := from_cache.bits.data

    val lb_rdata  = Wire(UInt(DATA_WIDTH.W))
    val lbu_rdata = Wire(UInt(DATA_WIDTH.W))
    val lh_rdata  = Wire(UInt(DATA_WIDTH.W))
    val lhu_rdata = Wire(UInt(DATA_WIDTH.W))
    val lw_rdata  = Wire(UInt(DATA_WIDTH.W))

    lb_rdata := MuxLookup(addr_low_2, 0.U)(List(
        0.U -> Cat(Fill(24, rdata_align_4(7 )), rdata_align_4(7, 0)),
        1.U -> Cat(Fill(24, rdata_align_4(15)), rdata_align_4(15, 8)),
        2.U -> Cat(Fill(24, rdata_align_4(23)), rdata_align_4(23, 16)),
        3.U -> Cat(Fill(24, rdata_align_4(31)), rdata_align_4(31, 24)),
    ))

    lbu_rdata := MuxLookup(addr_low_2, 0.U)(List(
        0.U -> Cat(Fill(24, 0.U), rdata_align_4(7, 0)),
        1.U -> Cat(Fill(24, 0.U), rdata_align_4(15, 8)),
        2.U -> Cat(Fill(24, 0.U), rdata_align_4(23, 16)),
        3.U -> Cat(Fill(24, 0.U), rdata_align_4(31, 24)),
    ))

    lh_rdata := MuxLookup(addr_low_2, 0.U)(List(
        0.U -> Cat(Fill(16, rdata_align_4(15)), rdata_align_4(15, 0 )),
        2.U -> Cat(Fill(16, rdata_align_4(31)), rdata_align_4(31, 16)),
    ))

    lhu_rdata := MuxLookup(addr_low_2, 0.U)(List(
        0.U -> Cat(Fill(16, 0.U), rdata_align_4(15, 0 )),
        2.U -> Cat(Fill(16, 0.U), rdata_align_4(31, 16)),
    ))

    lw_rdata := rdata_align_4

    // store inst
    val sb_wmask = Wire(UInt((DATA_WIDTH).W))
    val sh_wmask = Wire(UInt((DATA_WIDTH).W))
    val sw_wmask = Wire(UInt((DATA_WIDTH).W))

    sb_wmask    :=  MuxLookup(addr_low_2, 0.U)(List(
        0.U -> 0x000000ffL.U,
        1.U -> 0x0000ff00L.U,
        2.U -> 0x00ff0000L.U,
        3.U -> 0xff000000L.U,
    ))

    sh_wmask    :=  MuxLookup(addr_low_2, 0.U)(List(
        0.U -> 0x0000ffffL.U,
        2.U -> 0xffff0000L.U,
    ))

    sw_wmask    :=  0xffffffffL.U

    val wmask   =  MuxLookup(lsu_op, 0.U)(List(
        ("b" + lsu_sb).U -> sb_wmask,
        ("b" + lsu_sh).U -> sh_wmask,
        ("b" + lsu_sw).U -> sw_wmask,
    ))

    io.out.idle  := MuxLookup(state_lsu, false.B)(List(s_idle -> true.B))
    io.out.end   := MuxLookup(state_lsu, false.B)(List(s_end  -> true.B))
    io.out.rdata    :=  MuxLookup(lsu_op, 0.U)(List(
        ("b" + lsu_x  ).U -> 0.U,
        ("b" + lsu_lb ).U -> lb_rdata,
        ("b" + lsu_lbu).U -> lbu_rdata,
        ("b" + lsu_lh ).U -> lh_rdata,
        ("b" + lsu_lhu).U -> lhu_rdata,
        ("b" + lsu_lw ).U -> lw_rdata,
    ))

    from_cache.ready       := MuxLookup(state_lsu, false.B)(List(s_read_wait -> true.B, s_write_wait -> true.B))

    // axi master signals
    to_cache.valid         := MuxLookup(state_lsu, false.B)(List(s_read_request  -> true.B, s_write_request -> true.B))
    to_cache.bits.addr     := io.in.addr
    to_cache.bits.wdata    := io.in.wdata
    to_cache.bits.wmask    := wmask
    to_cache.bits.is_write := MuxLookup(state_lsu, false.B)(List(s_write_request  -> true.B, s_write_wait -> true.B))
}

// lsu with simplebus
class Lsu_simpleBus extends Module {
    val io = IO(new Bundle {
        val in  = (new ram_in_class )
        val out = (new ram_out_class)
    })
    val to_mem     = IO(new SimpleBus)

    // state_lsu machine, issue aw signals in write_request, issue w signals in write_wait
    val s_idle :: s_read_request :: s_read_wait :: s_write_request :: s_write_wait :: s_end :: Nil = Enum(6)
    val state_lsu = RegInit(s_idle)
    switch (state_lsu) {
        is (s_idle) {
            when (io.in.valid) {
                when (io.in.mem_wen) {
                    state_lsu := s_write_request
                } .otherwise {
                    state_lsu := s_read_request
                }
            } .otherwise {
                state_lsu := s_idle
            }
        }
        is (s_read_request) {
            state_lsu := Mux(to_mem.req.fire, s_read_wait, s_read_request)
        }
        is (s_read_wait) {
            state_lsu := Mux(to_mem.resp.fire, s_end, s_read_wait)
        }
        is (s_write_request) {
            // maybe write should not wait? it issue request, and then do other ting.
            // this could be faster.
            state_lsu := Mux(to_mem.req.fire, s_write_wait, s_write_request)
        }
        is (s_write_wait) {
            state_lsu := Mux(to_mem.resp.fire && to_mem.resp.bits.wresp, s_end, s_write_wait)
        }
        is (s_end) {
            state_lsu := s_idle
        }
    }

    val lsu_op = io.in.op
    val true_addr = io.in.addr

    val addr_low_2 = true_addr(1, 0)

    val valid = io.in.valid

    val rdata_align_4 = Wire(UInt(DATA_WIDTH.W))
    rdata_align_4 := to_mem.resp.bits.rdata

    val lb_rdata  = Wire(UInt(DATA_WIDTH.W))
    val lbu_rdata = Wire(UInt(DATA_WIDTH.W))
    val lh_rdata  = Wire(UInt(DATA_WIDTH.W))
    val lhu_rdata = Wire(UInt(DATA_WIDTH.W))
    val lw_rdata  = Wire(UInt(DATA_WIDTH.W))

    lb_rdata := MuxLookup(addr_low_2, 0.U)(List(
        0.U -> Cat(Fill(24, rdata_align_4(7 )), rdata_align_4(7, 0)),
        1.U -> Cat(Fill(24, rdata_align_4(15)), rdata_align_4(15, 8)),
        2.U -> Cat(Fill(24, rdata_align_4(23)), rdata_align_4(23, 16)),
        3.U -> Cat(Fill(24, rdata_align_4(31)), rdata_align_4(31, 24)),
    ))

    lbu_rdata := MuxLookup(addr_low_2, 0.U)(List(
        0.U -> Cat(Fill(24, 0.U), rdata_align_4(7, 0)),
        1.U -> Cat(Fill(24, 0.U), rdata_align_4(15, 8)),
        2.U -> Cat(Fill(24, 0.U), rdata_align_4(23, 16)),
        3.U -> Cat(Fill(24, 0.U), rdata_align_4(31, 24)),
    ))

    lh_rdata := MuxLookup(addr_low_2, 0.U)(List(
        0.U -> Cat(Fill(16, rdata_align_4(15)), rdata_align_4(15, 0 )),
        2.U -> Cat(Fill(16, rdata_align_4(31)), rdata_align_4(31, 16)),
    ))

    lhu_rdata := MuxLookup(addr_low_2, 0.U)(List(
        0.U -> Cat(Fill(16, 0.U), rdata_align_4(15, 0 )),
        2.U -> Cat(Fill(16, 0.U), rdata_align_4(31, 16)),
    ))

    lw_rdata := rdata_align_4

    // store inst
    val sb_wmask = Wire(UInt((DATA_WIDTH).W))
    val sh_wmask = Wire(UInt((DATA_WIDTH).W))
    val sw_wmask = Wire(UInt((DATA_WIDTH).W))

    sb_wmask    :=  MuxLookup(addr_low_2, 0.U)(List(
        0.U -> 0x000000ffL.U,
        1.U -> 0x0000ff00L.U,
        2.U -> 0x00ff0000L.U,
        3.U -> 0xff000000L.U,
    ))

    sh_wmask    :=  MuxLookup(addr_low_2, 0.U)(List(
        0.U -> 0x0000ffffL.U,
        2.U -> 0xffff0000L.U,
    ))

    sw_wmask    :=  0xffffffffL.U

    val wmask   =  MuxLookup(lsu_op, 0.U)(List(
        ("b" + lsu_sb).U -> sb_wmask,
        ("b" + lsu_sh).U -> sh_wmask,
        ("b" + lsu_sw).U -> sw_wmask,
    ))

    io.out.idle  := MuxLookup(state_lsu, false.B)(List(s_idle -> true.B))
    io.out.end   := MuxLookup(state_lsu, false.B)(List(s_end  -> true.B))
    io.out.rdata    :=  MuxLookup(lsu_op, 0.U)(List(
        ("b" + lsu_x  ).U -> 0.U,
        ("b" + lsu_lb ).U -> lb_rdata,
        ("b" + lsu_lbu).U -> lbu_rdata,
        ("b" + lsu_lh ).U -> lh_rdata,
        ("b" + lsu_lhu).U -> lhu_rdata,
        ("b" + lsu_lw ).U -> lw_rdata,
    ))

    to_mem.resp.ready       := MuxLookup(state_lsu, false.B)(List(s_read_wait -> true.B, s_write_wait -> true.B))

    // axi master signals
    to_mem.req.valid         := MuxLookup(state_lsu, false.B)(List(s_read_request  -> true.B, s_write_request -> true.B))
    to_mem.req.bits.addr     := io.in.addr
    to_mem.req.bits.wdata    := io.in.wdata
    to_mem.req.bits.wmask    := wmask
    to_mem.req.bits.cmd      := MuxLookup(state_lsu, 0.U)(List(
                                    s_read_request   -> SimpleBusCmd.read,
                                    s_read_wait      -> SimpleBusCmd.read,
                                    s_write_request  -> SimpleBusCmd.write, 
                                    s_write_wait     -> SimpleBusCmd.write
                                ))
    to_mem.req.bits.len      := 0.U
    to_mem.req.bits.wlast    := true.B
}

class LSUPipeline extends Module {

}
