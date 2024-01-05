package rv32e.core.backend.fu

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._

import rv32e.core.config._
import rv32e.core.define.Dec_Info._

class bru_in_class extends Bundle {
    val src1    =   Input(UInt(DATA_WIDTH.W))
    val src2    =   Input(UInt(DATA_WIDTH.W))
    val op  =   Input(UInt(BRUOP_WIDTH.W))
}


class bru_out_class extends Bundle {
    val ctrl_br =   Output(Bool())
}

class Bru extends Module {
    val io = IO(new Bundle {
        val in  =   (new bru_in_class)
        val out =   (new bru_out_class) 
    })

    val op          =   io.in.op
    val operand1    =   io.in.src1 
    val operand2    =   io.in.src2

    io.out.ctrl_br  :=   MuxLookup(op, 0.U)(List(
        ("b" + bru_jal ).U  ->   true.B,
        ("b" + bru_jalr).U  ->   true.B,
        ("b" + bru_beq ).U  ->   (operand1 === operand2),
        ("b" + bru_bne ).U  ->   (operand1 =/= operand2),
        ("b" + bru_blt ).U  ->   (operand1.asSInt <  operand2.asSInt),
        ("b" + bru_bge ).U  ->   (operand1.asSInt >= operand2.asSInt),
        ("b" + bru_bltu).U  ->   (operand1.asUInt <  operand2.asUInt),
        ("b" + bru_bgeu).U  ->   (operand1.asUInt >= operand2.asUInt),
    ))
}
