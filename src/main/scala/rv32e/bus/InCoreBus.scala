// Here is in core buses connected modules in a cpu core
package rv32e.bus

import chisel3._
import chisel3.util._
import rv32e.config.Configs._
import rv32e.define.Dec_Info._

// ifu to idu bus
class IFU2IDU_bus extends Bundle {
    val inst    =   Output(UInt(ADDR_WIDTH.W)) 
    val pc      =   Output(UInt(ADDR_WIDTH.W))       
}

/* contrl signals */
class IDU_CtrlSig extends Bundle {
    val reg_wen   = Output(Bool()) // idu -> isu -> exu -> wbu -> isu, used in back to isu stage
    val fu_op     = Output(UInt(FU_TYPEOP_WIDTH.W)) // used in wb stage
    val mem_wen   = Output(Bool())
    val is_ebreak = Output(Bool())
    val not_impl  = Output(Bool())
    val src1_op   = Output(UInt(SRCOP_WIDTH.W))
    val src2_op   = Output(UInt(SRCOP_WIDTH.W))
    val alu_op    = Output(UInt(ALUOP_WIDTH.W))
    val lsu_op    = Output(UInt(LSUOP_WIDTH.W))
    val bru_op    = Output(UInt(BRUOP_WIDTH.W))
    val csr_op    = Output(UInt(CSROP_WIDTH.W))
    val mdu_op    = Output(UInt(MDUOP_WIDTH.W))
}

class IDU2ISU_bus extends Bundle {
    val imm      = Output(UInt(DATA_WIDTH.W))
    val pc       = Output(UInt(ADDR_WIDTH.W))
    val rs1      = Output(UInt(REG_OP_WIDTH.W))
    val rs2      = Output(UInt(REG_OP_WIDTH.W))
    val rd       = Output(UInt(REG_OP_WIDTH.W))
    val ctrl_sig = new IDU_CtrlSig
}

class ISU2EXU_bus extends Bundle {
    val imm      = Output(UInt(DATA_WIDTH.W))
    val pc       = Output(UInt(ADDR_WIDTH.W))
    val rdata1   = Output(UInt(DATA_WIDTH.W))
    val rdata2   = Output(UInt(DATA_WIDTH.W))
    val ctrl_sig = new IDU_CtrlSig
}

class EXU2WBU_bus extends Bundle {
    val alu_result = Output(UInt(DATA_WIDTH.W))
    val mdu_result = Output(UInt(DATA_WIDTH.W))
    val lsu_rdata  = Output(UInt(DATA_WIDTH.W))
    val csr_rdata  = Output(UInt(DATA_WIDTH.W))
    val pc         = Output(UInt(ADDR_WIDTH.W))
    val reg_wen    = Output(Bool()) // idu -> isu -> exu -> wbu -> isu
    val fu_op     = Output(UInt(FU_TYPEOP_WIDTH.W)) // used in wb stage
}

class EXU2IFU_bus extends Bundle {
    val bru_ctrl_br     = Output(Bool()) 
    val bru_addr   = Output(UInt(ADDR_WIDTH.W)) // from alu
    val csr_ctrl_br     = Output(Bool())
    val csr_addr   = Output(UInt(ADDR_WIDTH.W))
}

class WBU2ISU_bus extends Bundle {
    val reg_wen   = Output(Bool())
    val wdata     = Output(UInt(DATA_WIDTH.W))
}
