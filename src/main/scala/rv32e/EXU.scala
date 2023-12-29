package rv32e

import chisel3._
import chisel3.util._
import rv32e.define.Dec_Info._
import rv32e.fu._
import rv32e.bus._
import rv32e.utils.DiffCsr
import rv32e.device.SRAM
import rv32e.bus.Arbiter
import rv32e.bus.AXILiteIO_master
import rv32e.bus.AXILiteIO_slave
import chisel3.util.experimental.BoringUtils


// if have no new inst come in the regs before exu, should clear it.
class EXU_pipeline extends Module {
    val from_ISU    = IO(Flipped(Decoupled(new ISU2EXU_bus)))
    val to_WBU      = IO(Decoupled(new EXU2WBU_bus))
    val difftest    = IO(new DiffCsr)
    val lsu_to_mem  = IO(new SimpleBus)
    // val lsu_to_mem  = IO(new AXI4)
    val to_ISU      = IO(new EXU2ISU_bus)

    val Alu_i             = Module(new Alu())
    val Mdu_i             = Module(new Mdu())
    val Bru_i             = Module(new Bru())
    // val Lsu_i             = Module(new Lsu_simpleBus())
    val Lsu_i             = Module(new LSUPipeline())
    val Csr_i             = Module(new Csr())

    // alu
    Alu_i.io.in.op := from_ISU.bits.ctrl_sig.alu_op
    Alu_i.io.in.src1   := MuxLookup(from_ISU.bits.ctrl_sig.src1_op, 0.U)(List(
        ("b"+src_rf).U  ->  from_ISU.bits.rdata1,
        ("b"+src_pc).U  ->  from_ISU.bits.pc,
    ))
    Alu_i.io.in.src2   := MuxLookup(from_ISU.bits.ctrl_sig.src2_op, 0.U)(List(
        ("b"+src_rf).U   ->  from_ISU.bits.rdata2,
        ("b"+src_imm).U  ->  from_ISU.bits.imm,
    ))

    // mdu
    Mdu_i.io.in.op   := from_ISU.bits.ctrl_sig.mdu_op
    Mdu_i.io.in.src1 := from_ISU.bits.rdata1
    Mdu_i.io.in.src2 := from_ISU.bits.rdata2

    // lsu
    Lsu_i.io.in.addr    := Alu_i.io.out.result
    Lsu_i.io.in.wdata   := from_ISU.bits.rdata2
    Lsu_i.io.in.mem_wen := from_ISU.bits.ctrl_sig.mem_wen
    Lsu_i.io.in.op      := from_ISU.bits.ctrl_sig.lsu_op
    Lsu_i.io.in.valid   := from_ISU.bits.isLSU && from_ISU.valid
    lsu_to_mem          <> Lsu_i.to_mem
    // lsu_to_mem          <> Lsu_i.axi

    // bru
    Bru_i.io.in.op     := from_ISU.bits.ctrl_sig.bru_op
    Bru_i.io.in.src1   := from_ISU.bits.rdata1
    Bru_i.io.in.src2   := from_ISU.bits.rdata2

    // csr
    Csr_i.io.in.op      :=  from_ISU.bits.ctrl_sig.csr_op
    Csr_i.io.in.cur_pc  :=  from_ISU.bits.pc
    Csr_i.io.in.csr_id  :=  from_ISU.bits.imm
    Csr_i.io.in.wdata   :=  from_ISU.bits.rdata1

    // to isu, the logic here should be simplified
    from_ISU.ready := to_WBU.ready && MuxLookup(from_ISU.bits.ctrl_sig.fu_op, true.B)(List(
        /**
          * from_ISU is not valid means the result has been committed to WB, so we are ready to receive the
          * next inst.
          */ 
        ("b"+fu_lsu).U -> (~from_ISU.valid || Lsu_i.io.out.end),
    ))

    // to wbu, logical not right here.
    to_WBU.valid := from_ISU.valid && MuxLookup(from_ISU.bits.ctrl_sig.fu_op, true.B)(List(
        ("b"+fu_lsu).U -> Lsu_i.io.out.end,
    ))
    to_WBU.bits.alu_result := Alu_i.io.out.result
    to_WBU.bits.mdu_result := Mdu_i.io.out.result
    to_WBU.bits.lsu_rdata  := Lsu_i.io.out.rdata
    to_WBU.bits.csr_rdata  := Csr_i.io.out.r_csr
    to_WBU.bits.pc         := from_ISU.bits.pc
    to_WBU.bits.inst       := from_ISU.bits.inst
    to_WBU.bits.reg_wen    := from_ISU.bits.ctrl_sig.reg_wen
    to_WBU.bits.fu_op      := from_ISU.bits.ctrl_sig.fu_op
    to_WBU.bits.rd         := from_ISU.bits.rd
    to_WBU.bits.redirect.valid  := (Bru_i.io.out.ctrl_br || Csr_i.io.out.csr_br) && from_ISU.valid
    to_WBU.bits.redirect.target := MuxLookup(from_ISU.bits.ctrl_sig.fu_op, 0.U)(List(
        ("b"+fu_bru).U -> Alu_i.io.out.result,
        ("b"+fu_csr).U -> Csr_i.io.out.csr_addr
    ))
    to_WBU.bits.is_ebreak  := from_ISU.bits.ctrl_sig.is_ebreak
    to_WBU.bits.not_impl   := from_ISU.bits.ctrl_sig.not_impl

    // to isu
    to_ISU.hazard.rd      := from_ISU.bits.rd
    to_ISU.hazard.have_wb := ~from_ISU.valid
    to_ISU.hazard.isBR    := from_ISU.bits.isBRU || from_ISU.bits.isCSR

    difftest <> Csr_i.io.out.difftest

    BoringUtils.addSource(from_ISU.bits.pc, "id3")
    BoringUtils.addSource(from_ISU.bits.inst, "id4")
}
