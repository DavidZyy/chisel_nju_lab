package rv32e

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.config.Dec_Info._
import rv32e.config.Inst._

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

class RegFile extends Module {
    val io = IO(new Bundle {
        val rf_in  = (new rf_in_class)
        val rf_out = (new rf_out_class)
    })

    val regs = RegInit(VecInit(Seq.fill(16)(0.U(32.W))))

    io.rf_out.rdata1    :=  regs(io.rf_in.rs1)
    io.rf_out.rdata2    :=  regs(io.rf_in.rs2)

    when (io.rf_in.reg_wen && io.rf_in.rd =/= 0.U) {
        regs(io.rf_in.rd)   :=  io.rf_in.wdata
    }
}

// object decoder_main extends App {
//     emitVerilog(new RegFile(), Array("--target-dir", "generated"))
//     // emitVerilog(new WriteSmem(), Array("--target-dir", "generated"))
// }
