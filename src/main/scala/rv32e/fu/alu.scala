package rv32e.fu

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.define.Dec_Info._
import rv32e.define.Inst._

class alu_in_class extends Bundle {
    val src1    =   Input(UInt(DATA_WIDTH.W))
    val src2    =   Input(UInt(DATA_WIDTH.W))
    val op  =   Input(UInt(ALUOP_WIDTH.W))
}

class alu_out_class extends Bundle {
    val result = Output(UInt(DATA_WIDTH.W))
}

class Alu extends Module {
    val io = IO(new Bundle {
        val in  =   (new alu_in_class)
        val out =   (new alu_out_class)
    })

    val alu_op   = io.in.op
    val operand1 = io.in.src1
    val operand2 = io.in.src2

    val shamt = operand2(4, 0)

    io.out.result   :=
    MuxLookup(alu_op, 0.U)(List(
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