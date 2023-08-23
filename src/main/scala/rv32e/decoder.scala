package rv32e

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.config.Dec_Info._
import rv32e.config.Inst._

/* contrl signals */
class contrl_signals extends Bundle {
    // val mem_wen =   Output(UInt(1.W))
    val mem_wen =   Output(Bool())
    // val reg_wen =   Output(UInt(1.W))
    val reg_wen =   Output(Bool())
    val src1_op =   Output(UInt(SRCOP_WIDTH.W))
    val src2_op =   Output(UInt(SRCOP_WIDTH.W))
    val alu_op  =   Output(UInt(ALUOP_WIDTH.W))
    val fu_op   =   Output(UInt(FU_TYPEOP_WIDTH.W))
    val lsu_op  =   Output(UInt(LSUOP_WIDTH.W))
    val bru_op  =   Output(UInt(BRUOP_WIDTH.W))
}

class decoder_out extends Bundle {
    val imm      = Output(UInt(DATA_WIDTH.W))
    val rs1      = Output(UInt(REG_OP_WIDTH.W))
    val rs2      = Output(UInt(REG_OP_WIDTH.W))
    val rd       = Output(UInt(REG_OP_WIDTH.W))
    val ctrl_sig = new contrl_signals
}

/* decode info:
    inst_type, alu_op, src1, src2, reg_wen, mem_wen,
 */
class Decoder extends Module {
    val io = IO(new Bundle {
        val inst    =   Input(UInt(INST_WIDTH.W))
        val out     =   new decoder_out
    })

    val imm_i   =   Cat(Fill(20, io.inst(31)), io.inst(31, 20))
    val imm_s   =   Cat(Fill(20, io.inst(31)), io.inst(31, 25), io.inst(11, 7))
    val imm_b   =   Cat(Fill(20, io.inst(31)), io.inst(7), io.inst(30, 25), io.inst(11, 8), 0.U(1.W))
    val imm_u   =   Cat(io.inst(31, 12), Fill(12, 0.U))
    val imm_j   =   Cat(Fill(12, io.inst(31)), io.inst(31), io.inst(19, 12), io.inst(20), io.inst(30, 21), Fill(1, 0.U))

    val table = TruthTable(
        Map(
        /* 2.4 Integer Computational Instructions */
            /* Integer Register-Immediate Instructions */
ADDI    ->  BitPat("b" + fu_alu + lsu_x + bru_x + i_type + alu_add  + src_rf + src_imm + reg_w_yes + mem_w_no),
SLTI    ->  BitPat("b" + fu_alu + lsu_x + bru_x + i_type + alu_slt  + src_rf + src_imm + reg_w_yes + mem_w_no),
SLTIU   ->  BitPat("b" + fu_alu + lsu_x + bru_x + i_type + alu_sltu + src_rf + src_imm + reg_w_yes + mem_w_no),
ANDI    ->  BitPat("b" + fu_alu + lsu_x + bru_x + i_type + alu_and  + src_rf + src_imm + reg_w_yes + mem_w_no),
ORI     ->  BitPat("b" + fu_alu + lsu_x + bru_x + i_type + alu_or   + src_rf + src_imm + reg_w_yes + mem_w_no),
XORI    ->  BitPat("b" + fu_alu + lsu_x + bru_x + i_type + alu_xor  + src_rf + src_imm + reg_w_yes + mem_w_no),
SLLI    ->  BitPat("b" + fu_alu + lsu_x + bru_x + i_type + alu_sll  + src_rf + src_imm + reg_w_yes + mem_w_no),
SRLI    ->  BitPat("b" + fu_alu + lsu_x + bru_x + i_type + alu_srl  + src_rf + src_imm + reg_w_yes + mem_w_no),
SRAI    ->  BitPat("b" + fu_alu + lsu_x + bru_x + i_type + alu_sra  + src_rf + src_imm + reg_w_yes + mem_w_no),
LUI     ->  BitPat("b" + fu_alu + lsu_x + bru_x + u_type + alu_add  + src_x  + src_imm + reg_w_yes + mem_w_no),
AUIPC   ->  BitPat("b" + fu_alu + lsu_x + bru_x + u_type + alu_add  + src_pc + src_imm + reg_w_yes + mem_w_no),

            /* Integer Register-Register Instructions */
ADD     ->  BitPat("b" + fu_alu + lsu_x + bru_x + r_type + alu_add  + src_rf + src_rf  + reg_w_yes + mem_w_no),
SLT     ->  BitPat("b" + fu_alu + lsu_x + bru_x + r_type + alu_slt  + src_rf + src_rf  + reg_w_yes + mem_w_no),
SLTU    ->  BitPat("b" + fu_alu + lsu_x + bru_x + r_type + alu_sltu + src_rf + src_rf  + reg_w_yes + mem_w_no),
AND     ->  BitPat("b" + fu_alu + lsu_x + bru_x + r_type + alu_and  + src_rf + src_rf  + reg_w_yes + mem_w_no),
OR      ->  BitPat("b" + fu_alu + lsu_x + bru_x + r_type + alu_or   + src_rf + src_rf  + reg_w_yes + mem_w_no),
XOR     ->  BitPat("b" + fu_alu + lsu_x + bru_x + r_type + alu_xor  + src_rf + src_rf  + reg_w_yes + mem_w_no),
SLL     ->  BitPat("b" + fu_alu + lsu_x + bru_x + r_type + alu_sll  + src_rf + src_rf  + reg_w_yes + mem_w_no),
SRL     ->  BitPat("b" + fu_alu + lsu_x + bru_x + r_type + alu_srl  + src_rf + src_rf  + reg_w_yes + mem_w_no),
SUB     ->  BitPat("b" + fu_alu + lsu_x + bru_x + r_type + alu_sub  + src_rf + src_rf  + reg_w_yes + mem_w_no),
SRA     ->  BitPat("b" + fu_alu + lsu_x + bru_x + r_type + alu_sra  + src_rf + src_rf  + reg_w_yes + mem_w_no),

        /* 2.5 Control Transfer Instructions */
            /* Unconditional Jumps */
JAL     ->  BitPat("b" + fu_alu + lsu_x + bru_jal  + j_type + alu_add + src_pc + src_imm + reg_w_yes + mem_w_no),
JALR    ->  BitPat("b" + fu_alu + lsu_x + bru_jalr + i_type + alu_add + src_rf + src_imm + reg_w_yes + mem_w_no),

            /* Conditional Branches */
BEQ     ->  BitPat("b" + fu_alu + lsu_x + bru_beq  + b_type + alu_add + src_pc + src_imm + reg_w_no + mem_w_no),
BNE     ->  BitPat("b" + fu_alu + lsu_x + bru_bne  + b_type + alu_add + src_pc + src_imm + reg_w_no + mem_w_no),
BLT     ->  BitPat("b" + fu_alu + lsu_x + bru_blt  + b_type + alu_add + src_pc + src_imm + reg_w_no + mem_w_no),
BGE     ->  BitPat("b" + fu_alu + lsu_x + bru_bge  + b_type + alu_add + src_pc + src_imm + reg_w_no + mem_w_no),
BLTU    ->  BitPat("b" + fu_alu + lsu_x + bru_bltu + b_type + alu_add + src_pc + src_imm + reg_w_no + mem_w_no),
BGEU    ->  BitPat("b" + fu_alu + lsu_x + bru_bgeu + b_type + alu_add + src_pc + src_imm + reg_w_no + mem_w_no),

        /* 2.6 Load and Store Instructions */
LB      ->  BitPat("b" + fu_alu + lsu_lb  + bru_x + i_type + alu_add + src_rf + src_imm + reg_w_yes + mem_w_no),
LBU     ->  BitPat("b" + fu_alu + lsu_lbu + bru_x + i_type + alu_add + src_rf + src_imm + reg_w_yes + mem_w_no),
LH      ->  BitPat("b" + fu_alu + lsu_lh  + bru_x + i_type + alu_add + src_rf + src_imm + reg_w_yes + mem_w_no),
LHU     ->  BitPat("b" + fu_alu + lsu_lhu + bru_x + i_type + alu_add + src_rf + src_imm + reg_w_yes + mem_w_no),
LW      ->  BitPat("b" + fu_alu + lsu_lw  + bru_x + i_type + alu_add + src_rf + src_imm + reg_w_yes + mem_w_no),
SB      ->  BitPat("b" + fu_alu + lsu_sb  + bru_x + s_type + alu_add + src_rf + src_imm + reg_w_no  + mem_w_yes),
SH      ->  BitPat("b" + fu_alu + lsu_sh  + bru_x + s_type + alu_add + src_rf + src_imm + reg_w_no  + mem_w_yes),
SW      ->  BitPat("b" + fu_alu + lsu_sw  + bru_x + s_type + alu_add + src_rf + src_imm + reg_w_no  + mem_w_yes),

        /* 2.8 Environment Call and Breakpoints */
EBREAK  ->  BitPat("b" + fu_alu + lsu_x + bru_x + no_type + alu_x + src_x + src_x + reg_w_no + mem_w_no)
        ),
            BitPat("b" + fu_alu + lsu_x + bru_x + no_type + alu_x + src_x + src_x + reg_w_no + mem_w_no)
    )

    val decode_info =   decoder(io.inst, table)

    val inst_type   =   decode_info(INST_TYPEOP_MSB, INST_TYPEOP_LSB)
    io.out.imm := MuxLookup(inst_type, 0.U, Array(
        ("b"+ i_type).U -> imm_i,
        ("b"+ s_type).U -> imm_s,
        ("b"+ b_type).U -> imm_b,
        ("b"+ u_type).U -> imm_u,
        ("b"+ j_type).U -> imm_j
    ))

    io.out.ctrl_sig.mem_wen  :=  decode_info(MEM_W_OP_MSB, MEM_W_OP_LSB)
    io.out.ctrl_sig.reg_wen  :=  decode_info(REG_W_OP_MSB, REG_W_OP_LSB)
    io.out.ctrl_sig.src2_op  :=  decode_info(SRC2_MSB, SRC2_LSB)
    io.out.ctrl_sig.src1_op  :=  decode_info(SRC1_MSB, SRC1_LSB)
    io.out.ctrl_sig.alu_op   :=  decode_info(ALUOP_MSB, ALUOP_LSB)
    io.out.ctrl_sig.bru_op   :=  decode_info(BRUOP_MSB, BRUOP_LSB)
    io.out.ctrl_sig.lsu_op   :=  decode_info(LSUOP_MSB, LSUOP_LSB)
    io.out.ctrl_sig.fu_op    :=  decode_info(FU_TYPEOP_MSB, FU_TYPEOP_LSB)


    io.out.rs1 := io.inst(19, 15)
    io.out.rs2 := io.inst(24, 20)
    io.out.rd  := io.inst(11, 7)
}

// object decoder_main extends App {
//     emitVerilog(new Decoder(), Array("--target-dir", "generated"))
//     // emitVerilog(new WriteSmem(), Array("--target-dir", "generated"))
// }
