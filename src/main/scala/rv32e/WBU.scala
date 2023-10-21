package rv32e

// reg file put in here temporaly, no very smart, but trade-off for now
import chisel3._
import chisel3.util._
import rv32e.bus._
import rv32e.define.Dec_Info._
import rv32e.config.Configs._

class WBU extends Module {
    val from_EXU = IO(Flipped(Decoupled(new EXU2WBU_bus)))
    val to_ISU   = IO(Decoupled(new WBU2ISU_bus))
    val to_IFU   = IO(Decoupled(new WBU2IFU_bus))
    from_EXU.ready := true.B
    to_ISU.valid   := true.B
    to_IFU.valid   := from_EXU.valid // finish signal

    // if not valid, don't reg_wen !!
    to_ISU.bits.reg_wen := Mux(from_EXU.fire, from_EXU.bits.reg_wen, false.B)

    to_ISU.bits.wdata   := MuxLookup(from_EXU.bits.fu_op, 0.U, Array(
        ("b"+fu_alu).U  ->  from_EXU.bits.alu_result,
        ("b"+fu_lsu).U  ->  from_EXU.bits.lsu_rdata,
        ("b"+fu_bru).U  ->  (from_EXU.bits.pc + ADDR_BYTE_WIDTH.U),
        ("b"+fu_csr).U  ->  from_EXU.bits.csr_rdata,
        ("b"+fu_mdu).U  ->  from_EXU.bits.mdu_result,
    ))
}

