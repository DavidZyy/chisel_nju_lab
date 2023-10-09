package rv32e.bus

import chisel3._
import chisel3.util._
import rv32e.config.Configs._
import rv32e.config.Dec_Info._


// ifu to idu bus
class IFU2IDU_bus extends Bundle {
    val inst            =   Output(UInt(ADDR_WIDTH.W)) 
}

/* contrl signals */
class contrl_signals extends Bundle {
    val mem_wen   = Output(Bool())
    val reg_wen   = Output(Bool())
    val is_ebreak = Output(Bool())
    val not_impl  = Output(Bool())
    val src1_op   = Output(UInt(SRCOP_WIDTH.W))
    val src2_op   = Output(UInt(SRCOP_WIDTH.W))
    val alu_op    = Output(UInt(ALUOP_WIDTH.W))
    val fu_op     = Output(UInt(FU_TYPEOP_WIDTH.W))
    val lsu_op    = Output(UInt(LSUOP_WIDTH.W))
    val bru_op    = Output(UInt(BRUOP_WIDTH.W))
    val csr_op    = Output(UInt(CSROP_WIDTH.W))
    val mdu_op    = Output(UInt(MDUOP_WIDTH.W))
}

class IDU2ISU_bus extends Bundle {
    val imm      = Output(UInt(DATA_WIDTH.W))
    val rs1      = Output(UInt(REG_OP_WIDTH.W))
    val rs2      = Output(UInt(REG_OP_WIDTH.W))
    val rd       = Output(UInt(REG_OP_WIDTH.W))
    val ctrl_sig = new contrl_signals
}
