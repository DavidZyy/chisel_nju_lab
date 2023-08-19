package rv32e

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.config.Dec_Info._
import rv32e.config.Inst._
import empty.alu

class out_class extends Bundle {
    val  inst = Output(UInt(INST_WIDTH.W))
    val  pc   = Output(UInt(DATA_WIDTH.W))
}

class top extends Module {
    val io = IO(new Bundle{
        val out = (new out_class)
    })

    val PCReg_i     = Module(new PCReg())
    val Rom_i       = Module(new Rom())
    val Decoder_i   = Module(new Decoder())
    val RegFile_i   = Module(new RegFile())       
    val Alu_i       = Module(new Alu())
    val Bru_i       = Module(new Bru())
    val Ram_i       = Module(new Ram())

    // pc
    PCReg_i.io.ctrl_br := Bru_i.io.bru_out.ctrl_br
    PCReg_i.io.addr_target  := Alu_i.io.alu_out.alu_result
    
    // rom
    Rom_i.io.addr := PCReg_i.io.cur_pc

    // decoder
    Decoder_i.io.inst := Rom_i.io.inst

    // reg file
    RegFile_i.io.rf_in.rd  := Decoder_i.io.out.rd
    RegFile_i.io.rf_in.rs1 := Decoder_i.io.out.rs1
    RegFile_i.io.rf_in.rs2 := Decoder_i.io.out.rs2
    RegFile_i.io.rf_in.reg_wen := Decoder_i.io.out.ctrl_sig.reg_wen

    val is_load = 
        Decoder_i.io.out.ctrl_sig.lsu_op === ("b"+lsu_lb).U |
        Decoder_i.io.out.ctrl_sig.lsu_op === ("b"+lsu_lbu).U |
        Decoder_i.io.out.ctrl_sig.lsu_op === ("b"+lsu_lh).U |
        Decoder_i.io.out.ctrl_sig.lsu_op === ("b"+lsu_lhu).U |
        Decoder_i.io.out.ctrl_sig.lsu_op === ("b"+lsu_lw).U 

    val is_jump =
        Decoder_i.io.out.ctrl_sig.bru_op === ("b"+bru_jal).U |
        Decoder_i.io.out.ctrl_sig.bru_op === ("b"+bru_jalr).U

    when(is_load) {
        RegFile_i.io.rf_in.wdata := Ram_i.io.ram_out.rdata
    } .elsewhen(is_jump) {
        RegFile_i.io.rf_in.wdata := PCReg_i.io.cur_pc + ADDR_BYTE_WIDTH.U
    } .otherwise{
        RegFile_i.io.rf_in.wdata := Alu_i.io.alu_out.alu_result
    }

    // alu
    Alu_i.io.alu_in.alu_op := Decoder_i.io.out.ctrl_sig.alu_op

    when(Decoder_i.io.out.ctrl_sig.src1_op === ("b"+src_rf).U){
        Alu_i.io.alu_in.src1 := RegFile_i.io.rf_out.rdata1
    } .elsewhen(Decoder_i.io.out.ctrl_sig.src1_op === ("b"+src_pc).U) {
        Alu_i.io.alu_in.src1 := PCReg_i.io.cur_pc
    } .otherwise {
        Alu_i.io.alu_in.src1 := 0.U
    }

    when(Decoder_i.io.out.ctrl_sig.src2_op === ("b"+src_rf).U){
        Alu_i.io.alu_in.src2 := RegFile_i.io.rf_out.rdata2
    } .elsewhen(Decoder_i.io.out.ctrl_sig.src2_op === ("b"+src_imm).U) {
        Alu_i.io.alu_in.src2 := Decoder_i.io.out.imm
    } .otherwise {
        Alu_i.io.alu_in.src2 := 0.U
    }

    // bru
    Bru_i.io.bru_in.bru_op := Decoder_i.io.out.ctrl_sig.bru_op
    Bru_i.io.bru_in.src1   := RegFile_i.io.rf_out.rdata1
    Bru_i.io.bru_in.src2   := RegFile_i.io.rf_out.rdata2

    // ram
    Ram_i.io.ram_in.addr  := Alu_i.io.alu_out.alu_result
    Ram_i.io.ram_in.wdata := RegFile_i.io.rf_out.rdata2
    Ram_i.io.ram_in.mem_wen := Decoder_i.io.out.ctrl_sig.mem_wen
    Ram_i.io.ram_in.lsu_op  := Decoder_i.io.out.ctrl_sig.lsu_op

    io.out.inst := Rom_i.io.inst
    io.out.pc   := PCReg_i.io.cur_pc
}


object decoder_main extends App {
    emitVerilog(new top(), Array("--target-dir", "generated"))
    // emitVerilog(new WriteSmem(), Array("--target-dir", "generated"))
}