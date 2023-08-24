package rv32e

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.config.Dec_Info._
import rv32e.config.Inst._
import empty.alu

class ram_in_class extends Bundle {
    val mem_ren =   Input(Bool())
    val mem_wen =   Input(Bool())
    val addr    =   Input(UInt(ADDR_WIDTH.W))
    val wdata   =   Input(UInt(DATA_WIDTH.W))
    val lsu_op  =   Input(UInt(LSUOP_WIDTH.W))
}

class ram_out_class extends Bundle {
    val rdata   =   Output(UInt(DATA_WIDTH.W))
}

class RamBB extends BlackBox with HasBlackBoxResource {
    val io = IO(new Bundle {
        val clock = Input(Clock())
        val addr  = Input(UInt(DATA_WIDTH.W))
        val wdata = Input(UInt(DATA_WIDTH.W))
        val mem_wen = Input(Bool())
        val valid   = Input(Bool())
        // val mem_ren = Input(Bool())
        val rdata = Output(UInt(DATA_WIDTH.W))
        val rdata_4_w = Output(UInt(DATA_WIDTH.W))
    })
    addResource("/RamBB.v")
}

class Ram extends Module {
    val io = IO(new Bundle {
        val in  = (new ram_in_class )
        val out = (new ram_out_class)
    })

    val lsu_op = io.in.lsu_op
    val true_addr = io.in.addr

    val addr_low_2 = true_addr(1, 0) 

    val valid = io.in.mem_ren | io.in.mem_wen

    val RamBB_i1 = Module(new RamBB())

    RamBB_i1.io.clock   := clock
    RamBB_i1.io.addr    := (io.in.addr >> 2) << 2 // align to 4
    RamBB_i1.io.mem_wen := io.in.mem_wen
    RamBB_i1.io.valid   := valid
    // RamBB_i1.io.wdata   := wdata_align_4
    val rdata_align_4 = RamBB_i1.io.rdata
    val rdata_4_w     = RamBB_i1.io.rdata_4_w
    // val mem = Mem(MEM_INST_SIZE, UInt(DATA_WIDTH.W))

    // val rdata_align_4 = mem.read(true_addr >> 2)

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

    val sb_wmask = Wire(UInt(DATA_WIDTH.W))
    val sh_wmask = Wire(UInt(DATA_WIDTH.W))
    val sw_wmask = Wire(UInt(DATA_WIDTH.W))

    sb_wmask    :=  MuxLookup(addr_low_2, 0.U, Array(
        0.U -> 0x000000ffL.U,
        1.U -> 0x0000ff00L.U,
        2.U -> 0x00ff0000L.U,
        3.U -> 0xff000000L.U,
    ))

    sh_wmask    :=  MuxLookup(addr_low_2, 0.U, Array(
        0.U -> 0x0000ffffL.U,
        2.U -> 0xffff0000L.U,
    ))

    sw_wmask    :=  0xffffffffL.U

    val wmask   =  MuxLookup(lsu_op, 0.U, Array(
        ("b" + lsu_sb).U -> sb_wmask,
        ("b" + lsu_sh).U -> sh_wmask,
        ("b" + lsu_sw).U -> sw_wmask,
    ))

    val wdata_align_4 = 
        // 0.U
        // (((io.in.wdata << (8.U * addr_low_2)) & wmask) | (rdata_align_4))
        (((io.in.wdata << (8.U * addr_low_2)) & wmask) | (rdata_4_w & ~wmask))

    RamBB_i1.io.wdata   := wdata_align_4

    // when (io.in.mem_wen) {
    //     mem.write(true_addr >> 2.U, 
    //     ((io.in.wdata << (8.U * addr_low_2)) & wmask | rdata_align_4 & ~wmask))
    // }
}


// object decoder_main extends App {
//     emitVerilog(new Ram(), Array("--target-dir", "generated"))
//     // emitVerilog(new WriteSmem(), Array("--target-dir", "generated"))
// }