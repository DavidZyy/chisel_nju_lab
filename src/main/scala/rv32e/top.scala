package rv32e

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.config.Dec_Info._
import rv32e.config.Inst._
import empty.alu
import rv32e.fu.CSR
import rv32e.fu.Mdu
import java.awt.MouseInfo

class CSRs extends Bundle {
  val mcause  = Output(UInt(DATA_WIDTH.W))
  val mepc    = Output(UInt(DATA_WIDTH.W))
  val mstatus = Output(UInt(DATA_WIDTH.W))
  val mtvec   = Output(UInt(DATA_WIDTH.W))
}

class out_class extends Bundle {
    val  inst     = Output(UInt(INST_WIDTH.W))
    val  pc       = Output(UInt(DATA_WIDTH.W))
    val  is_load  = Output(Bool())
    val  difftest = new CSRs
}

class top extends Module {
    val io = IO(new Bundle{
        val out = (new out_class)
    })

    val PCReg_i             = Module(new PCReg())
    val Rom_i               = Module(new Rom())
    val Decoder_i           = Module(new Decoder())
    val RegFile_i           = Module(new RegFile())       
    val Alu_i               = Module(new Alu())
    val Bru_i               = Module(new Bru())
    val Ram_i               = Module(new Ram())
    val ebreak_moudle_i     = Module(new ebreak_moudle())
    val not_impl_moudle_i   = Module(new not_impl_moudle())
    val csr_i               = Module(new CSR())
    val Mdu_i               = Module(new Mdu())

    // pc
    PCReg_i.io.in.ctrl_br      := Bru_i.io.bru_out.ctrl_br
    PCReg_i.io.in.addr_target  := Alu_i.io.alu_out.alu_result
    PCReg_i.io.in.ctrl_csr     := csr_i.io.out.ctrl_csr
    PCReg_i.io.in.excpt_addr   := csr_i.io.out.csr_pc
    
    // rom
    Rom_i.io.addr := PCReg_i.io.out.cur_pc

    // decoder
    Decoder_i.io.inst := Rom_i.io.inst

    // reg file
    RegFile_i.io.in.rd  := Decoder_i.io.out.rd
    RegFile_i.io.in.rs1 := Decoder_i.io.out.rs1
    RegFile_i.io.in.rs2 := Decoder_i.io.out.rs2
    RegFile_i.io.in.reg_wen := Decoder_i.io.out.ctrl_sig.reg_wen

    val is_load = 
        Decoder_i.io.out.ctrl_sig.lsu_op === ("b"+lsu_lb).U |
        Decoder_i.io.out.ctrl_sig.lsu_op === ("b"+lsu_lbu).U |
        Decoder_i.io.out.ctrl_sig.lsu_op === ("b"+lsu_lh).U |
        Decoder_i.io.out.ctrl_sig.lsu_op === ("b"+lsu_lhu).U |
        Decoder_i.io.out.ctrl_sig.lsu_op === ("b"+lsu_lw).U 

    val is_jump =
        Decoder_i.io.out.ctrl_sig.bru_op === ("b"+bru_jal).U |
        Decoder_i.io.out.ctrl_sig.bru_op === ("b"+bru_jalr).U
    
    val wcsr    =
        Decoder_i.io.out.ctrl_sig.csr_op === ("b"+csr_csrrs).U |
        Decoder_i.io.out.ctrl_sig.csr_op === ("b"+csr_csrrw).U

    // fu_type can be used here to determine which outcome of the fu should be written to regfile
    when(is_load) {
        RegFile_i.io.in.wdata := Ram_i.io.out.rdata
    } .elsewhen(is_jump) {
        RegFile_i.io.in.wdata := PCReg_i.io.out.cur_pc + ADDR_BYTE_WIDTH.U
    } .elsewhen(wcsr) {
        RegFile_i.io.in.wdata := csr_i.io.out.r_csr
    } .elsewhen(Decoder_i.io.out.ctrl_sig.fu_op === ("b"+fu_mdu).U) {
        RegFile_i.io.in.wdata := Mdu_i.io.out.mdu_result
    } .otherwise {
        RegFile_i.io.in.wdata := Alu_i.io.alu_out.alu_result
    } 

    // alu
    Alu_i.io.alu_in.alu_op := Decoder_i.io.out.ctrl_sig.alu_op

    when(Decoder_i.io.out.ctrl_sig.src1_op === ("b"+src_rf).U){
        Alu_i.io.alu_in.src1 := RegFile_i.io.out.rdata1
    } .elsewhen(Decoder_i.io.out.ctrl_sig.src1_op === ("b"+src_pc).U) {
        Alu_i.io.alu_in.src1 := PCReg_i.io.out.cur_pc
    } .otherwise {
        Alu_i.io.alu_in.src1 := 0.U
    }

    when(Decoder_i.io.out.ctrl_sig.src2_op === ("b"+src_rf).U){
        Alu_i.io.alu_in.src2 := RegFile_i.io.out.rdata2
    } .elsewhen(Decoder_i.io.out.ctrl_sig.src2_op === ("b"+src_imm).U) {
        Alu_i.io.alu_in.src2 := Decoder_i.io.out.imm
    } .otherwise {
        Alu_i.io.alu_in.src2 := 0.U
    }

    // mdu
    Mdu_i.io.in.mdu_op := Decoder_i.io.out.ctrl_sig.mdu_op
    Mdu_i.io.in.src1   := RegFile_i.io.out.rdata1
    Mdu_i.io.in.src2   := RegFile_i.io.out.rdata2

    // bru
    Bru_i.io.bru_in.bru_op := Decoder_i.io.out.ctrl_sig.bru_op
    Bru_i.io.bru_in.src1   := RegFile_i.io.out.rdata1
    Bru_i.io.bru_in.src2   := RegFile_i.io.out.rdata2

    // ram
    Ram_i.io.in.addr    := Alu_i.io.alu_out.alu_result
    Ram_i.io.in.wdata   := RegFile_i.io.out.rdata2
    Ram_i.io.in.mem_wen := Decoder_i.io.out.ctrl_sig.mem_wen
    Ram_i.io.in.lsu_op  := Decoder_i.io.out.ctrl_sig.lsu_op
    Ram_i.io.in.mem_ren := is_load

    // csr
    csr_i.io.in.csr_op  :=  Decoder_i.io.out.ctrl_sig.csr_op
    csr_i.io.in.cur_pc  :=  PCReg_i.io.out.cur_pc
    csr_i.io.in.csr_id  :=  Decoder_i.io.out.imm
    csr_i.io.in.wdata   :=  RegFile_i.io.out.rdata1

    // ebreak
    ebreak_moudle_i.is_ebreak := Decoder_i.io.out.ctrl_sig.is_ebreak
    
    // not implemented
    not_impl_moudle_i.not_impl := Decoder_i.io.out.ctrl_sig.not_impl

    io.out.inst    := Rom_i.io.inst
    io.out.pc      := PCReg_i.io.out.cur_pc
    io.out.is_load := is_load

    io.out.difftest.mcause  := csr_i.io.out.difftest.mcause  
    io.out.difftest.mepc    := csr_i.io.out.difftest.mepc    
    io.out.difftest.mstatus := csr_i.io.out.difftest.mstatus  
    io.out.difftest.mtvec   := csr_i.io.out.difftest.mtvec   
}

object top_main extends App {
    emitVerilog(new top(), Array("--target-dir", "generated/cpu"))
    // emitVerilog(new WriteSmem(), Array("--target-dir", "generated"))
}