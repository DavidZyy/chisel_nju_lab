package rv32e.core.backend

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

import rv32e.bus._
import rv32e.bus.simplebus._

import rv32e.core.config._
import rv32e.core.define.Dec_Info._
import rv32e.core.define.Mem._
import rv32e.core.backend.fu._

import rv32e.utils.DiffCsr

class EXU_pipeline extends Module {
    val from_ISU    = IO(Flipped(Decoupled(new ISU2EXU_bus)))
    val to_WBU      = IO(Decoupled(new EXU2WBU_bus))
    val difftest    = IO(new DiffCsr)
    val lsu_to_mem  = IO(new SimpleBus)
    // val lsu_to_mem  = IO(new AXI4)
    val to_ISU      = IO(new EXU2ISU_bus)
    val npc         = IO(Input(UInt(ADDR_WIDTH.W)))

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
    Lsu_i.io.in.req.valid      := from_ISU.bits.isLSU && from_ISU.valid
    Lsu_i.io.in.req.bits.addr  := Alu_i.io.out.result
    Lsu_i.io.in.req.bits.wdata := from_ISU.bits.rdata2
    Lsu_i.io.in.req.bits.wmask := "b1111".U
    Lsu_i.io.in.req.bits.cmd   := Mux(from_ISU.bits.ctrl_sig.mem_wen, SimpleBusCmd.write, SimpleBusCmd.read)
    Lsu_i.io.in.req.bits.len   := 0.U
    Lsu_i.io.in.req.bits.wlast := true.B
    Lsu_i.io.in.resp.ready     := from_ISU.bits.isLSU
    Lsu_i.io.lsu_op            := from_ISU.bits.ctrl_sig.lsu_op
    lsu_to_mem                 <> Lsu_i.io.mem

    // bru
    Bru_i.io.in.op     := from_ISU.bits.ctrl_sig.bru_op
    Bru_i.io.in.src1   := from_ISU.bits.rdata1
    Bru_i.io.in.src2   := from_ISU.bits.rdata2

    // csr
    Csr_i.io.in.op      :=  from_ISU.bits.ctrl_sig.csr_op
    Csr_i.io.in.cur_pc  :=  from_ISU.bits.pc
    Csr_i.io.in.csr_id  :=  from_ISU.bits.imm
    Csr_i.io.in.wdata   :=  from_ISU.bits.rdata1

    val lsuStore = MuxLookup(from_ISU.bits.ctrl_sig.lsu_op, false.B)(List(
        ("b"+lsu_sb).U -> true.B,
        ("b"+lsu_sh).U -> true.B,
        ("b"+lsu_sw).U -> true.B,
        ("b"+lsu_sd).U -> true.B,
    ))

    // to isu, the logic here should be simplified
    from_ISU.ready := to_WBU.ready && MuxLookup(from_ISU.bits.ctrl_sig.fu_op, true.B)(List(
        /**
          * from_ISU is not valid means the result has been committed to WB, so we are ready to receive the
          * next inst.
          */ 
        // ("b"+fu_lsu).U -> Mux(lsuStore, Lsu_i.io.in.req.ready, (!from_ISU.valid || Lsu_i.io.in.resp.valid)),
        ("b"+fu_lsu).U -> (!from_ISU.valid || Lsu_i.io.in.resp.valid),
    ))

    // to wbu, logical not right here.
    to_WBU.valid := from_ISU.valid && MuxLookup(from_ISU.bits.ctrl_sig.fu_op, true.B)(List(
        // ("b"+fu_lsu).U -> Mux(lsuStore, true.B, Lsu_i.io.in.resp.valid),
        ("b"+fu_lsu).U -> Lsu_i.io.in.resp.valid,
    ))

    // branch prediction judge
    val taken = (Bru_i.io.out.ctrl_br || Csr_i.io.out.csr_br) // jump is taken
    val predictWrong = (taken && npc =/= Alu_i.io.out.result && npc =/= Csr_i.io.out.csr_addr) ||
                        (!taken && (npc =/= from_ISU.bits.pc+ADDR_BYTE.U))

    to_WBU.bits.alu_result := Alu_i.io.out.result
    to_WBU.bits.mdu_result := Mdu_i.io.out.result
    to_WBU.bits.lsu_rdata  := Lsu_i.io.in.resp.bits.rdata
    to_WBU.bits.csr_rdata  := Csr_i.io.out.r_csr
    to_WBU.bits.pc         := from_ISU.bits.pc
    to_WBU.bits.inst       := from_ISU.bits.inst
    to_WBU.bits.reg_wen    := from_ISU.bits.ctrl_sig.reg_wen
    to_WBU.bits.fu_op      := from_ISU.bits.ctrl_sig.fu_op
    to_WBU.bits.rd         := from_ISU.bits.rd
    to_WBU.bits.redirect.valid  := predictWrong && (from_ISU.bits.isBRU || from_ISU.bits.isCSR) && from_ISU.valid 
    to_WBU.bits.redirect.target := MuxLookup(from_ISU.bits.ctrl_sig.fu_op, 0.U)(List(
        ("b"+fu_bru).U -> Mux(taken, Alu_i.io.out.result, from_ISU.bits.pc+ADDR_BYTE.U),
        ("b"+fu_csr).U -> Mux(taken, Csr_i.io.out.csr_addr, from_ISU.bits.pc+ADDR_BYTE.U),
    ))
    to_WBU.bits.is_ebreak  := from_ISU.bits.ctrl_sig.is_ebreak
    to_WBU.bits.not_impl   := from_ISU.bits.ctrl_sig.not_impl
    to_WBU.bits.is_mmio    := from_ISU.bits.isLSU && Lsu_i.io.in.req.bits.addr >= mmioBase.U

    // to isu
    to_ISU.hazard.rd      := from_ISU.bits.rd
    to_ISU.hazard.have_wb := !from_ISU.valid
    to_ISU.hazard.isBR    := from_ISU.bits.isBRU || from_ISU.bits.isCSR

    difftest <> Csr_i.io.out.difftest
    
    BoringUtils.addSource(WireInit(from_ISU.bits.pc), "EXUPC")
    BoringUtils.addSource(WireInit(from_ISU.bits.inst), "EXUInst")
}