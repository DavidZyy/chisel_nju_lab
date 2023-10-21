package rv32e.utils

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.define.Dec_Info._
import rv32e.define.Inst._
import chisel3.util.HasBlackBoxResource

class rf_in_class extends Bundle {
    val rs1     =   Input(UInt(REG_OP_WIDTH.W))
    val rs2     =   Input(UInt(REG_OP_WIDTH.W))
    val rd      =   Input(UInt(REG_OP_WIDTH.W))
    val wdata   =   Input(UInt(DATA_WIDTH.W))
    val reg_wen =   Input(Bool())
}

class rf_out_class extends Bundle {
    val rdata1 = Output(UInt(DATA_WIDTH.W))
    val rdata2 = Output(UInt(DATA_WIDTH.W))
}

class RegisterFileBB extends BlackBox with HasBlackBoxResource {
    val io = IO(new Bundle {
        val clock   =   Input(Clock())
        val reset   =   Input(Bool())
        val rs1     =   Input(UInt(REG_OP_WIDTH.W))
        val rs2     =   Input(UInt(REG_OP_WIDTH.W))
        val rd      =   Input(UInt(REG_OP_WIDTH.W))
        val wdata   =   Input(UInt(DATA_WIDTH.W))
        val reg_wen =   Input(Bool())

        val rdata1 = Output(UInt(DATA_WIDTH.W))
        val rdata2 = Output(UInt(DATA_WIDTH.W))
    })

    addResource("/RegisterFileBB.v")
}

class RegFile extends Module {
    val io = IO(new Bundle {
        val in  = (new rf_in_class)
        val out = (new rf_out_class)
    })

    val regfile = Module(new RegisterFileBB())

    regfile.io.clock   := clock
    regfile.io.reset   := reset
    regfile.io.rs1     := io.in.rs1
    regfile.io.rs2     := io.in.rs2
    regfile.io.rd      := io.in.rd
    regfile.io.wdata   := io.in.wdata
    regfile.io.reg_wen := io.in.reg_wen

    io.out.rdata1 := regfile.io.rdata1
    io.out.rdata2 := regfile.io.rdata2
}

// object decoder_main extends App {
//     emitVerilog(new RegFile(), Array("--target-dir", "generated/regfile"))
// }
