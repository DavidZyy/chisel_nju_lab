package rv32e.core.backend.fu

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._

import rv32e.bus.simplebus._

import rv32e.core.config._
import rv32e.core.define.Dec_Info._

class LSUPipeline extends Module {
    val io = IO(new Bundle {
        val in      = Flipped(new SimpleBus)
        val mem     = new SimpleBus
        val lsu_op  = Input(UInt(LSUOP_WIDTH.W))
    })

    val true_addr = io.in.req.bits.addr

    val addr_low_2 = true_addr(1, 0)

    val rdata_align_4 = Wire(UInt(DATA_WIDTH.W))
    rdata_align_4 := io.mem.resp.bits.rdata

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
    val sb_wmask = Wire(UInt(wmaskWidth.W))
    val sh_wmask = Wire(UInt(wmaskWidth.W))
    val sw_wmask = Wire(UInt(wmaskWidth.W))

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

    val wmask   =  MuxLookup(io.lsu_op, 0.U)(List(
        ("b" + lsu_sb).U -> sb_wmask,
        ("b" + lsu_sh).U -> sh_wmask,
        ("b" + lsu_sw).U -> sw_wmask,
    ))

    val rdata = MuxLookup(io.lsu_op, 0.U)(List(
        ("b" + lsu_x  ).U -> 0.U,
        ("b" + lsu_lb ).U -> lb_rdata,
        ("b" + lsu_lbu).U -> lbu_rdata,
        ("b" + lsu_lh ).U -> lh_rdata,
        ("b" + lsu_lhu).U -> lhu_rdata,
        ("b" + lsu_lw ).U -> lw_rdata,
    ))

    // io.in
    io.in.req.ready          := io.mem.req.ready
    io.in.resp.valid         := io.mem.resp.valid
    io.in.resp.bits.rdata    := rdata
    io.in.resp.bits.wresp    := DontCare

    // io.mem
    io.mem.req.valid         := io.in.req.valid
    io.mem.req.bits.addr     := io.in.req.bits.addr
    io.mem.req.bits.wdata    := io.in.req.bits.wdata
    io.mem.req.bits.wmask    := wmask
    io.mem.req.bits.cmd      := io.in.req.bits.cmd
    io.mem.req.bits.len      := io.in.req.bits.len
    io.mem.req.bits.wlast    := io.in.req.bits.wlast
    io.mem.resp.ready        := io.in.resp.ready
}
