// Here is in core buses connected modules in a cpu core
package rv32e.bus

import chisel3._
import chisel3.util._
import rv32e.config.Configs._
import rv32e.define.Dec_Info._
import rv32e.config.Axi_Configs._

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

    val inst    =   Output(UInt(ADDR_WIDTH.W)) // for debug
}

class ISU2EXU_bus extends Bundle {
    val imm      = Output(UInt(DATA_WIDTH.W))
    val pc       = Output(UInt(ADDR_WIDTH.W))
    val rdata1   = Output(UInt(DATA_WIDTH.W))
    val rdata2   = Output(UInt(DATA_WIDTH.W))
    val rd       = Output(UInt(REG_OP_WIDTH.W))
    val ctrl_sig = new IDU_CtrlSig

    def isLSU    = ctrl_sig.fu_op === ("b"+fu_lsu).U
    def isBRU    = ctrl_sig.fu_op === ("b"+fu_bru).U
    def isCSR    = ctrl_sig.fu_op === ("b"+fu_csr).U
    def isALU    = ctrl_sig.fu_op === ("b"+fu_alu).U
    def isMDU    = ctrl_sig.fu_op === ("b"+fu_mdu).U

    val inst     = Output(UInt(ADDR_WIDTH.W)) // for debug
}

class RedirectIO extends Bundle {
    val valid  = Output(Bool())
    val target = Output(UInt(ADDR_WIDTH.W))
}

class EXU2WBU_bus extends Bundle {
    val alu_result = Output(UInt(DATA_WIDTH.W))
    val mdu_result = Output(UInt(DATA_WIDTH.W))
    val lsu_rdata  = Output(UInt(DATA_WIDTH.W))
    val csr_rdata  = Output(UInt(DATA_WIDTH.W))
    val pc         = Output(UInt(ADDR_WIDTH.W))
    val reg_wen    = Output(Bool()) // idu -> isu -> exu -> wbu -> isu
    val rd         = Output(UInt(REG_OP_WIDTH.W))
    val fu_op      = Output(UInt(FU_TYPEOP_WIDTH.W)) // used in wb stage
    val redirect   = new RedirectIO

    val inst    =   Output(UInt(ADDR_WIDTH.W)) // for debug
}

class WBU2ISU_bus extends Bundle {
    val reg_wen   = Output(Bool())
    val wdata     = Output(UInt(DATA_WIDTH.W))
    val rd        = Output(UInt(REG_OP_WIDTH.W))
}

class WBU2IFU_bus extends Bundle {
    val redirect = new RedirectIO
}

class IFU2Cache_bus extends Bundle {
    val addr = Output(UInt(ADDR_WIDTH.W))
}

class Cache2IFU_bus extends Bundle {
    val data = Output(UInt(DATA_WIDTH.W))
}

class LSU2Cache_bus extends Bundle {
    val addr      = Output(UInt(ADDR_WIDTH.W))

    val wdata     = Output(UInt(DATA_WIDTH.W))
    val is_write  = Output(Bool())
    val wmask     = Output(UInt(DATA_WIDTH.W))
}

class Cache2LSU_bus extends Bundle {
    val data = Output(UInt(DATA_WIDTH.W))

    val bresp = Output(Bool())  // write response
}

class HazardIO extends Bundle {

}

class EXU2ISU_bus extends Bundle {
    val rd      = Output(UInt(REG_OP_WIDTH.W))
    val have_wb = Output(Bool())
    val isBR    = Output(Bool())
}

class PipelineDebugInfo extends Bundle {
    val inst    =   Output(UInt(ADDR_WIDTH.W))
    val pc      =   Output(UInt(ADDR_WIDTH.W))      
}
