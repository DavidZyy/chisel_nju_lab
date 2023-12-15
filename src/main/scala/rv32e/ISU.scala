package rv32e

import chisel3._
import chisel3.util._
import rv32e.bus._
import rv32e.utils.RegFile
import rv32e.define.Dec_Info._

// instruction issue unit, put register file in this unit
// reg file in here !
class ISU extends Module {
    val from_IDU = IO(Flipped(Decoupled(new IDU2ISU_bus)))
    val from_WBU = IO(Flipped(Decoupled(new WBU2ISU_bus)))
    val to_EXU   = IO(Decoupled(new ISU2EXU_bus))
    val from_EXU = IO(Flipped(new EXU2ISU_bus))

    val has_hazard = (from_EXU.rd === from_IDU.bits.rs1 || from_EXU.rd === from_IDU.bits.rs2) && ~from_EXU.have_wb && from_IDU.valid

    val RegFile_i           =  Module(new RegFile())
    RegFile_i.io.in.rs1     := from_IDU.bits.rs1
    RegFile_i.io.in.rs2     := from_IDU.bits.rs2
    RegFile_i.io.in.rd      := from_WBU.bits.rd
    RegFile_i.io.in.reg_wen := from_WBU.bits.reg_wen
    RegFile_i.io.in.wdata   := from_WBU.bits.wdata

    // to idu
    // from_IDU.ready := to_EXU.ready
    from_IDU.ready := ~has_hazard && to_EXU.ready

    // to wbu
    from_WBU.ready := true.B

    // to EXU
    // to_EXU.valid         := from_IDU.valid
    to_EXU.valid         := ~has_hazard && from_IDU.valid
    to_EXU.bits.ctrl_sig <> from_IDU.bits.ctrl_sig
    to_EXU.bits.imm      := from_IDU.bits.imm
    to_EXU.bits.pc       := from_IDU.bits.pc
    to_EXU.bits.rd       := from_IDU.bits.rd
    to_EXU.bits.rdata1   := RegFile_i.io.out.rdata1
    to_EXU.bits.rdata2   := RegFile_i.io.out.rdata2
    to_EXU.bits.inst     := from_IDU.bits.inst
}
