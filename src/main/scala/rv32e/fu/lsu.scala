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
    // val valid   =   Input(Bool())
    val mem_wen =   Output(Bool())
    val addr    =   Output(UInt(ADDR_WIDTH.W))
    val wdata   =   Output(UInt(DATA_WIDTH.W))
    val op      =   Output(UInt(LSUOP_WIDTH.W))
}

class ram_out_class extends Bundle {
    val rdata   =   Output(UInt(DATA_WIDTH.W))
    val end     =   Output(Bool())
    val idle    =   Output(Bool())
}

class LSUPipeline extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Decoupled(new ram_in_class ))
        val out = (new ram_out_class)
    })
    val to_mem     = IO(new SimpleBus)

    val lsu_op    = io.in.bits.op
    val true_addr = io.in.bits.addr

    val addr_low_2 = true_addr(1, 0)

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

    val wmask   =  MuxLookup(lsu_op, 0.U)(List(
        ("b" + lsu_sb).U -> sb_wmask,
        ("b" + lsu_sh).U -> sh_wmask,
        ("b" + lsu_sw).U -> sw_wmask,
    ))

    io.in.ready  := true.B

    io.out.idle  := to_mem.resp.valid
    io.out.end   := to_mem.resp.valid
    io.out.rdata    :=  MuxLookup(lsu_op, 0.U)(List(
        ("b" + lsu_x  ).U -> 0.U,
        ("b" + lsu_lb ).U -> lb_rdata,
        ("b" + lsu_lbu).U -> lbu_rdata,
        ("b" + lsu_lh ).U -> lh_rdata,
        ("b" + lsu_lhu).U -> lhu_rdata,
        ("b" + lsu_lw ).U -> lw_rdata,
    ))

    to_mem.resp.ready        := true.B

    to_mem.req.valid         := io.in.valid
    to_mem.req.bits.addr     := io.in.bits.addr
    to_mem.req.bits.wdata    := io.in.bits.wdata
    to_mem.req.bits.wmask    := wmask
    to_mem.req.bits.cmd      := Mux(io.in.bits.mem_wen, SimpleBusCmd.write, SimpleBusCmd.read)
    to_mem.req.bits.len      := 0.U
    to_mem.req.bits.wlast    := true.B
}
