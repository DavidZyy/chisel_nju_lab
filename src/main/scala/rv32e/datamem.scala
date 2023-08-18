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
    val mem_wen =   Input(Bool())
    val addr    =   Input(UInt(ADDR_WIDTH.W))
    val wdata   =   Input(UInt(DATA_WIDTH.W))
    val lsu_op  =   Input(UInt(LSUOP_WIDTH.W))
}

class ram_out_class extends Bundle {
    val rdata   =   Output(UInt(DATA_WIDTH.W))
}

class Ram extends Module {
    val io = IO(new Bundle {
        val ram_in  = (new ram_in_class )
        val ram_out = (new ram_out_class)
    })

    val lsu_op = io.ram_in.lsu_op

    val addr_low_2 = io.ram_in.addr(1, 0) 

    val mem = Mem(MEM_INST_SIZE, UInt(DATA_WIDTH.W))

    val rdata_align_4 = mem.read(io.ram_in.addr >> 2)

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

    io.ram_out.rdata    :=  MuxLookup(lsu_op, 0.U, Array(
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

    when (io.ram_in.mem_wen) {
        mem.write(io.ram_in.addr >> 2.U, 
        ((io.ram_in.wdata << (8.U * addr_low_2)) & wmask | rdata_align_4 & ~wmask))
    }
}


// object decoder_main extends App {
//     emitVerilog(new Ram(), Array("--target-dir", "generated"))
//     // emitVerilog(new WriteSmem(), Array("--target-dir", "generated"))
// }