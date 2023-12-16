package rv32e

import chisel3._
import chisel3.util._
import rv32e.define.Dec_Info._
import rv32e.fu._
import rv32e.bus._
import rv32e.utils.DiffCsr
import rv32e.device.SRAM
import rv32e.utils.StageConnect
import rv32e.bus.Arbiter
import rv32e.bus.AXILiteIO_master
import rv32e.bus.AXILiteIO_slave

class EXU extends Module {
    val from_ISU = IO(Flipped(Decoupled(new ISU2EXU_bus)))
    val to_WBU   = IO(Decoupled(new EXU2WBU_bus))
    val to_IFU   = IO(Decoupled(new EXU2IFU_bus))
    val difftest = IO(new DiffCsr)
    val lsu_to_mem        = IO(new SimpleBus)

    val Alu_i             = Module(new Alu())
    val Mdu_i             = Module(new Mdu())
    val Bru_i             = Module(new Bru())
    val Lsu_i             = Module(new Lsu_simpleBus())
    val Csr_i             = Module(new Csr())
    val ebreak_moudle_i   = Module(new ebreak_moudle())
    val not_impl_moudle_i = Module(new not_impl_moudle())

    // wait result now only wait lsu!!, for it's the slowest
    val s_idle :: s_wait_lsu :: s_end :: Nil = Enum(3)
    val state = RegInit(s_idle)
    switch (state) {
        is (s_idle) {
            when (from_ISU.fire) {
                when (from_ISU.bits.ctrl_sig.fu_op === ("b"+fu_lsu).U ) {
                    state := s_wait_lsu
                } .otherwise {
                    state := s_end
                }
            } .otherwise {
                state := s_idle
            }
        }
        is (s_wait_lsu) {
            state := Mux(Lsu_i.io.out.end, s_end, s_wait_lsu)
        }
        is (s_end) {
            // state := Mux(from_ISU.fire, s_end, s_idle) // continue execute
            state := s_idle
        }
    }
    from_ISU.ready := MuxLookup(state, false.B)(List(s_idle -> true.B))
    to_WBU.valid   := MuxLookup(state, false.B)(List(s_end  -> true.B))
    to_IFU.valid   := MuxLookup(state, false.B)(List(s_end  -> true.B))

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
    // Lsu_i.io.in.valid   := (from_ISU.bits.ctrl_sig.fu_op === ("b"+fu_lsu).U) && (state === s_end)
    Lsu_i.io.in.valid   := MuxLookup(state, false.B)(List(s_wait_lsu -> true.B))

    // bru
    Bru_i.io.in.op     := from_ISU.bits.ctrl_sig.bru_op
    Bru_i.io.in.src1   := from_ISU.bits.rdata1
    Bru_i.io.in.src2   := from_ISU.bits.rdata2

    // csr
    Csr_i.io.in.op      :=  from_ISU.bits.ctrl_sig.csr_op
    Csr_i.io.in.cur_pc  :=  from_ISU.bits.pc
    Csr_i.io.in.csr_id  :=  from_ISU.bits.imm
    // Csr_i.io.in.addr    :=  from_ISU.bits.imm
    Csr_i.io.in.wdata   :=  from_ISU.bits.rdata1

    // ebreak
    ebreak_moudle_i.is_ebreak  := from_ISU.bits.ctrl_sig.is_ebreak
    // not implemented
    not_impl_moudle_i.not_impl := from_ISU.bits.ctrl_sig.not_impl

    to_WBU.bits.alu_result := Alu_i.io.out.result
    to_WBU.bits.mdu_result := Mdu_i.io.out.result
    to_WBU.bits.lsu_rdata  := Lsu_i.io.out.rdata
    to_WBU.bits.csr_rdata  := Csr_i.io.out.r_csr
    to_WBU.bits.pc      := from_ISU.bits.pc
    to_WBU.bits.inst       := from_ISU.bits.inst
    to_WBU.bits.reg_wen := from_ISU.bits.ctrl_sig.reg_wen
    to_WBU.bits.fu_op   := from_ISU.bits.ctrl_sig.fu_op
    to_WBU.bits.rd      := from_ISU.bits.rd

    // to_IFU.bits.bru_ctrl_br := Bru_i.io.out.ctrl_br
    // to_IFU.bits.bru_addr    := Alu_i.io.out.result
    // to_IFU.bits.csr_ctrl_br := Csr_i.io.out.csr_br
    // to_IFU.bits.csr_addr    := Csr_i.io.out.csr_addr

    difftest <> Csr_i.io.out.difftest

    lsu_to_mem <> Lsu_i.to_mem
}

// if have no new inst come in the regs before exu, should clear it.
class EXU_pipeline extends Module {
    val from_ISU    = IO(Flipped(Decoupled(new ISU2EXU_bus)))
    val to_WBU      = IO(Decoupled(new EXU2WBU_bus))
    val to_IFU      = IO(Decoupled(new EXU2IFU_bus)) // redirection
    val difftest    = IO(new DiffCsr)
    val lsu_to_mem  = IO(new SimpleBus)
    val to_ISU      = IO(new EXU2ISU_bus)

    val Alu_i             = Module(new Alu())
    val Mdu_i             = Module(new Mdu())
    val Bru_i             = Module(new Bru())
    val Lsu_i             = Module(new Lsu_simpleBus())
    val Csr_i             = Module(new Csr())
    val ebreak_moudle_i   = Module(new ebreak_moudle())
    val not_impl_moudle_i = Module(new not_impl_moudle())

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

    // bru
    Bru_i.io.in.op     := from_ISU.bits.ctrl_sig.bru_op
    Bru_i.io.in.src1   := from_ISU.bits.rdata1
    Bru_i.io.in.src2   := from_ISU.bits.rdata2

    // csr
    Csr_i.io.in.op      :=  from_ISU.bits.ctrl_sig.csr_op
    Csr_i.io.in.cur_pc  :=  from_ISU.bits.pc
    Csr_i.io.in.csr_id  :=  from_ISU.bits.imm
    Csr_i.io.in.wdata   :=  from_ISU.bits.rdata1

    // ebreak
    ebreak_moudle_i.is_ebreak  := from_ISU.bits.ctrl_sig.is_ebreak && from_ISU.valid

    // not implemented
    not_impl_moudle_i.not_impl := from_ISU.bits.ctrl_sig.not_impl && from_ISU.valid

    // to isu
    from_ISU.ready := MuxLookup(from_ISU.bits.ctrl_sig.fu_op, true.B)(List(
        /**
          * from_ISU is not valid means the result has been committed to WB, so we are ready to receive the
          * next inst.
          */ 
        ("b"+fu_lsu).U -> (~from_ISU.valid || Lsu_i.io.out.end),
        // ("b"+fu_bru).U -> (~from_ISU.valid),
        // ("b"+fu_csr).U -> (~from_ISU.valid),
        ("b"+fu_bru).U -> to_IFU.ready,
        ("b"+fu_csr).U -> to_IFU.ready,
    ))

    // to wbu, logical not right here.
    // to_WBU.valid := from_ISU.fire && MuxLookup(from_ISU.bits.ctrl_sig.fu_op, true.B)(List(
    to_WBU.valid := from_ISU.valid && MuxLookup(from_ISU.bits.ctrl_sig.fu_op, true.B)(List(
        ("b"+fu_lsu).U -> Lsu_i.io.out.end,
        ("b"+fu_bru).U -> to_IFU.fire,
        ("b"+fu_csr).U -> to_IFU.fire, // write rf and pc at the same cycle
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

    // to ifu, branch control's wirte back signal should be put to IFU
    to_IFU.valid          := MuxLookup(from_ISU.bits.ctrl_sig.fu_op, false.B)(List(
        ("b"+fu_bru).U -> true.B,
        ("b"+fu_csr).U -> true.B
    ))
    to_IFU.bits.redirect := (Bru_i.io.out.ctrl_br || Csr_i.io.out.csr_br) && from_ISU.valid
    to_IFU.bits.target   := MuxLookup(from_ISU.bits.ctrl_sig.fu_op, 0.U)(List(
        ("b"+fu_bru).U -> Alu_i.io.out.result,
        ("b"+fu_csr).U -> Csr_i.io.out.csr_addr
    ))

    // to isu
    to_ISU.rd      := from_ISU.bits.rd
    to_ISU.have_wb := ~from_ISU.valid
    to_ISU.isBRU   := from_ISU.bits.isBRU

    difftest <> Csr_i.io.out.difftest
}
