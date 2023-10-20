package rv32e

import chisel3._
import chisel3.util._
import rv32e.define.Dec_Info._
import rv32e.fu._
import rv32e.bus._
import rv32e.utils.DiffCsr
import rv32e.dev.SRAM_lsu
import rv32e.utils.StageConnect

class EXU extends Module {
    val from_ISU = IO(Flipped(Decoupled(new ISU2EXU_bus)))
    val to_WBU   = IO(Decoupled(new EXU2WBU_bus))
    val to_IFU   = IO(Decoupled(new EXU2IFU_bus))
    val difftest = IO(new DiffCsr)

    val Alu_i               = Module(new Alu())
    val Mdu_i               = Module(new Mdu())
    val Bru_i               = Module(new Bru())
    val Lsu_i               = Module(new Lsu())
    val Csr_i               = Module(new Csr())
    val ebreak_moudle_i     = Module(new ebreak_moudle())
    val not_impl_moudle_i   = Module(new not_impl_moudle())

    // !!!!! to core change StageConnect !!!!!!!!!
    // from_ISU.ready := true.B
    // to_WBU.valid   := from_ISU.valid
    // to_IFU.valid   := true.B

    // wait result now only wait lsu!!, for it's the slowest
    val s_idle  :: s_begin :: s_wait_lsu :: s_end :: Nil = Enum(4)
    val state = RegInit(s_idle)
    switch (state) {
        is (s_idle) {
            state := Mux(from_ISU.fire, s_begin, s_idle)
            // state := Mux(from_ISU.fire, s_end, s_idle)
        }
        is (s_begin) {
            when (from_ISU.bits.ctrl_sig.fu_op === ("b"+fu_lsu).U ) {
                state := Mux(Lsu_i.io.out.idle, s_wait_lsu, s_begin)
            } .otherwise {
                state := s_end
            }
        }
        is (s_wait_lsu) {
            state := Mux(Lsu_i.io.out.end, s_end, s_wait_lsu)
        }
        is (s_end) {
            state := s_idle
        }
    }
    from_ISU.ready := MuxLookup(state, false.B, List(s_idle -> true.B))
    to_WBU.valid   := MuxLookup(state, false.B, List(s_end  -> true.B))
    to_IFU.valid   := MuxLookup(state, false.B, List(s_end  -> true.B))

    // alu
    Alu_i.io.in.op := from_ISU.bits.ctrl_sig.alu_op
    Alu_i.io.in.src1   := MuxLookup(from_ISU.bits.ctrl_sig.src1_op, 0.U, Array(
        ("b"+src_rf).U  ->  from_ISU.bits.rdata1,
        ("b"+src_pc).U  ->  from_ISU.bits.pc,
    ))
    Alu_i.io.in.src2   := MuxLookup(from_ISU.bits.ctrl_sig.src2_op, 0.U, Array(
        ("b"+src_rf).U   ->  from_ISU.bits.rdata2,
        ("b"+src_imm).U  ->  from_ISU.bits.imm,
    ))

    // mdu
    Mdu_i.io.in.op     := from_ISU.bits.ctrl_sig.mdu_op
    Mdu_i.io.in.src1   := from_ISU.bits.rdata1
    Mdu_i.io.in.src2   := from_ISU.bits.rdata2

    // lsu
    Lsu_i.io.in.addr    := Alu_i.io.out.result
    Lsu_i.io.in.wdata   := from_ISU.bits.rdata2
    Lsu_i.io.in.mem_wen := from_ISU.bits.ctrl_sig.mem_wen
    Lsu_i.io.in.op      := from_ISU.bits.ctrl_sig.lsu_op
    // Lsu_i.io.in.valid   := (from_ISU.bits.ctrl_sig.fu_op === ("b"+fu_lsu).U) && (state === s_end)
    Lsu_i.io.in.valid   := MuxLookup(state, false.B, List(s_wait_lsu -> true.B))

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
    ebreak_moudle_i.is_ebreak  := from_ISU.bits.ctrl_sig.is_ebreak
    // not implemented
    not_impl_moudle_i.not_impl := from_ISU.bits.ctrl_sig.not_impl

    to_WBU.bits.alu_result := Alu_i.io.out.result
    to_WBU.bits.mdu_result := Mdu_i.io.out.result
    to_WBU.bits.lsu_rdata  := Lsu_i.io.out.rdata
    to_WBU.bits.csr_rdata  := Csr_i.io.out.r_csr
    to_WBU.bits.pc         := from_ISU.bits.pc
    to_WBU.bits.reg_wen    := from_ISU.bits.ctrl_sig.reg_wen
    to_WBU.bits.fu_op      := from_ISU.bits.ctrl_sig.fu_op

    to_IFU.bits.bru_ctrl_br     := Bru_i.io.out.ctrl_br
    to_IFU.bits.bru_addr        := Alu_i.io.out.result
    to_IFU.bits.csr_ctrl_br     := Csr_i.io.out.csr_br
    to_IFU.bits.csr_addr        := Csr_i.io.out.csr_addr

    difftest <> Csr_i.io.out.difftest

    // val sram_i = Module(new SRAM_lsu())
    // StageConnect(Lsu_i.axi.ar, sram_i.axi.ar)
    // StageConnect(sram_i.axi.r, Lsu_i.axi.r)
    // StageConnect(Lsu_i.axi.aw, sram_i.axi.aw)
    // StageConnect(Lsu_i.axi.w,  sram_i.axi.w)
    // StageConnect(sram_i.axi.b, Lsu_i.axi.b)
}
