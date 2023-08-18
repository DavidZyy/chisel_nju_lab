package rv32e

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.config.Dec_Info._
import rv32e.config.Inst._
import empty.alu

class alu_in_class extends Bundle {
    val src1    =   Input(UInt(DATA_WIDTH.W))
    val src2    =   Input(UInt(DATA_WIDTH.W))
    val alu_op  =   Input(UInt(ALUOP_WIDTH.W))
}

class alu_out_class extends Bundle {
    val alu_result = Output(UInt(DATA_WIDTH.W))
}

class Alu extends Module {
    val io = IO(new Bundle {
        val alu_in  =   (new alu_in_class)
        val alu_out =   (new alu_out_class)
    })

    val alu_op   = io.alu_in.alu_op
    val operand1 = io.alu_in.src1
    val operand2 = io.alu_in.src2

    val shamt = operand2(4, 0)

    io.alu_out.alu_result   :=
    MuxLookup(alu_op, 0.U, Array(
        ("b" + alu_x   ).U  -> 0.U,
        ("b" + alu_add ).U  -> (operand1 + operand2).asUInt,
        ("b" + alu_sub ).U  -> (operand1 - operand2).asUInt,
        ("b" + alu_and ).U  -> (operand1 & operand2).asUInt,
        ("b" + alu_or  ).U  -> (operand1 | operand2).asUInt,
        ("b" + alu_xor ).U  -> (operand1 ^ operand2).asUInt,
        ("b" + alu_slt ).U  -> (operand1.asSInt < operand2.asSInt).asUInt,
        ("b" + alu_sltu).U  -> (operand1 < operand2).asUInt,
        ("b" + alu_sll ).U  -> (operand1 << shamt).asUInt,
        ("b" + alu_srl ).U  -> (operand1 >> shamt).asUInt,
        ("b" + alu_sra ).U  -> (operand1.asSInt >> shamt).asUInt,
    ))
}