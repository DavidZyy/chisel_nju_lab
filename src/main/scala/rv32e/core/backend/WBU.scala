package rv32e.core.backend

// reg file put in here temporaly, no very smart, but trade-off for now
import chisel3._
import chisel3.util._

import rv32e.bus._
import rv32e.core.config._
import rv32e.core.define.Dec_Info._
import rv32e.core.define.Mem._
import rv32e.core.backend.fu._
import rv32e.utils._

class WBU extends Module {
    val from_EXU = IO(Flipped(Decoupled(new EXU2WBU_bus)))
    val to_ISU   = IO(Decoupled(new WBU2ISU_bus))
    val to_IFU   = IO(Decoupled(new WBU2IFU_bus))
    val wb       = IO(Output(Bool()))
    val is_mmio  = IO(Output(Bool()))

    wb := from_EXU.valid

    // to exu
    from_EXU.ready := MuxLookup(from_EXU.bits.fu_op, true.B)(List(
        ("b"+fu_bru).U -> to_IFU.ready,
        ("b"+fu_csr).U -> to_IFU.ready,
    ))

    // to ifu
    to_IFU.valid := from_EXU.valid && MuxLookup(from_EXU.bits.fu_op, false.B)(List(
        ("b"+fu_bru).U -> true.B,
        ("b"+fu_csr).U -> true.B,
    ))
    to_IFU.bits.redirect.valid  := from_EXU.bits.redirect.valid && from_EXU.valid
    to_IFU.bits.redirect.target := from_EXU.bits.redirect.target
    to_IFU.bits.pc              := from_EXU.bits.pc

    // to isu
    to_ISU.valid        := from_EXU.valid
    to_ISU.bits.reg_wen := Mux(from_EXU.fire, from_EXU.bits.reg_wen, false.B)
    to_ISU.bits.rd      := from_EXU.bits.rd
    to_ISU.bits.wdata   := MuxLookup(from_EXU.bits.fu_op, 0.U)(List(
        ("b"+fu_alu).U  ->  from_EXU.bits.alu_result,
        ("b"+fu_lsu).U  ->  from_EXU.bits.lsu_rdata,
        ("b"+fu_bru).U  ->  (from_EXU.bits.pc + ADDR_BYTE.U),
        ("b"+fu_csr).U  ->  from_EXU.bits.csr_rdata,
        ("b"+fu_mdu).U  ->  from_EXU.bits.mdu_result,
    ))
    to_ISU.bits.hazard.rd      := from_EXU.bits.rd
    to_ISU.bits.hazard.have_wb := !from_EXU.valid
    to_ISU.bits.hazard.isBR    := from_EXU.bits.isBRU || from_EXU.bits.isCSR

    val ebreak_moudle_i   = Module(new ebreak_moudle())
    val not_impl_moudle_i = Module(new not_impl_moudle())

    // ebreak
    ebreak_moudle_i.valid   := from_EXU.bits.is_ebreak

    // not implemented
    not_impl_moudle_i.valid := from_EXU.bits.not_impl

    is_mmio := from_EXU.bits.is_mmio
    // Debug(wb, "[wbu], pc:%x, inst:%x\n", from_EXU.bits.pc, from_EXU.bits.inst)
}
