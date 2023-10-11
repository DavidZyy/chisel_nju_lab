package rv32e.fu

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.config.Dec_Info._
import rv32e.config.Inst._

class mdu_in_class extends Bundle {
    val src1    =   Input(UInt(DATA_WIDTH.W))
    val src2    =   Input(UInt(DATA_WIDTH.W))
    val op      =   Input(UInt(MDUOP_WIDTH.W))
}

class mdu_out_class extends Bundle {
    val result = Output(UInt(DATA_WIDTH.W))
}

class Mdu extends Module {
    val io = IO(new Bundle {
        val in  =   (new mdu_in_class)
        val out =   (new mdu_out_class)
    })

    val mdu_op   = io.in.op
    val operand1 = io.in.src1
    val operand2 = io.in.src2

    val shamt = operand2(4, 0)

    io.out.result   :=
    MuxLookup(mdu_op, 0.U, Array(
        ("b" + mdu_x     ).U  -> 0.U,
        ("b" + mdu_mul   ).U  -> (operand1 * operand2).asUInt,
        ("b" + mdu_mulh  ).U  -> ((operand1.asSInt * operand2.asSInt) >> DATA_WIDTH).asUInt,
        ("b" + mdu_mulhsu).U  -> ((operand1.asSInt * operand2) >> DATA_WIDTH).asUInt,
        ("b" + mdu_mulhu ).U  -> ((operand1 * operand2) >> DATA_WIDTH).asUInt,
        ("b" + mdu_div   ).U  -> (operand1.asSInt / operand2.asSInt).asUInt,
        ("b" + mdu_divu  ).U  -> (operand1 / operand2).asUInt,
        ("b" + mdu_rem   ).U  -> (operand1.asSInt % operand2.asSInt).asUInt,
        ("b" + mdu_remu  ).U  -> (operand1 % operand2).asUInt,
    ))
}
