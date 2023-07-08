package empty

import chisel3._
import chisel3.util._
import empty.Path

class shift_reg extends Module {
    val io = IO(new Bundle {
        val in  = Input(UInt(1.W))
        val op  = Input(UInt(3.W))
        val out = Output(UInt(8.W))
    })

    val reg = RegInit(0.U(8.W))
    val output_reg = RegInit(0.U(8.W))
    val cnt = RegInit(0.U(3.W))
    // val nextvalue = 0.U
    val nextvalue = Wire(UInt(8.W))
    // var cnt = 0.U

    when (io.op === "b000".U) {
        nextvalue   :=  0.U
    }.elsewhen( io.op === "b001".U) {
        nextvalue   :=  1.U
    }.elsewhen( io.op === "b010".U) {
        // logic shift right
        // nextvalue   :=  nextvalue   >>  1
        nextvalue   :=  Cat(0.U, reg(7,1))
    }.elsewhen( io.op === "b011".U) {
        nextvalue   :=  reg <<  1
    }.elsewhen( io.op === "b100".U) {
        nextvalue   :=  reg >>  1
    }.elsewhen( io.op === "b101".U) {
        nextvalue   :=  Cat(reg(6, 0), io.in)

        when (cnt === 7.U) {
            output_reg  := nextvalue 
            cnt    :=  0.U
        }.otherwise {
            cnt    := cnt + 1.U
        }

    }.elsewhen( io.op === "b110".U) {
        nextvalue   :=  Cat(reg(0), reg(7,1))
    }.elsewhen( io.op === "b111".U) {
        nextvalue   :=  Cat(reg(6,0), reg(7))
    }.otherwise {
        nextvalue   :=  reg //Default assignment
    }

    reg    :=   nextvalue
    io.out :=   output_reg
}

object shift_reg_main extends App {
    val rela_path: String = "lab6/shift_reg/vsrc"
    println("generating the shift_reg hardware")
    emitVerilog(new shift_reg(), Array("--target-dir", Path.path + rela_path))
}

class barrel_shift extends Module {
    val io = IO(new Bundle {
        val din     =   Input(UInt(8.W))
        val shamt   =   Input(UInt(3.W))
        val L_R     =   Input(Bool()) // true is shift left, false is shift right
        val A_L     =   Input(Bool()) // true is arithmetic shift, false is logic shift
        val dout    =   Output(UInt(8.W))
    })

    val shifted =   Wire(UInt(8.W))

    when ( io.L_R ) {
        shifted :=  io.din  <<  io.shamt
    }.otherwise {
        when (io.A_L) {
            shifted :=  (io.din.asSInt  >>  io.shamt).asUInt
        }.otherwise {
            shifted :=  io.din  >>  io.shamt
        }
    }

    io.dout  :=  shifted
}

object barrel_shift_main extends App {
    val rela_path: String = "lab6/barrel_shift/vsrc"
    println("generating the barrel_shift hardware")
    emitVerilog(new barrel_shift(), Array("--target-dir", Path.path + rela_path))
}


class LFSR extends Module {
    val io = IO(new Bundle {
        val dout =   Output(UInt(8.W))
    })

    val reg = RegInit(1.U(8.W))

    reg :=  Cat(reg(4) ^ reg(3) ^ reg(2) ^ reg(0), reg(7, 1))

    io.dout :=  reg
}

object LFSR_main extends App {
    val rela_path: String = "lab6/LFSR/vsrc"
    println("generating rela_path hardware")
    emitVerilog(new LFSR(), Array("--target-dir", Path.path + rela_path))
}
