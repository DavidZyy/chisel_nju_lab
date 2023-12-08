package empty

import chisel3._
import chisel3.util._
import empty.Path

// object Path {
//   val path: String = "/home/zhuyangyang/project/nju_digital_design_chisel/"
// }

// waste me a lot of time.
// A and B are complements
class alu extends Module {
    val io = IO(new Bundle {
        val A        = Input(UInt(4.W))
        val B        = Input(UInt(4.W))
        val op       = Input(UInt(3.W))
        val Result   = Output(UInt(4.W))
        val zero     = Output(UInt(1.W))
        val carry    = Output(UInt(1.W))
        val overflow = Output(UInt(1.W))
    })

    io.Result   := 0.U
    io.zero     := 0.U
    io.carry    := 0.U
    io.overflow := 0.U

    io.Result   := MuxLookup(io.op, 0.U)(Seq(
        "b000".U -> (io.A + io.B),
        "b001".U -> (io.A - io.B),
        "b010".U -> ~(io.A),
        "b011".U -> (io.A & io.B),
        "b100".U -> (io.A | io.B),
        "b101".U -> (io.A ^ io.B),
        "b110".U -> (io.A < io.B),
        "b111".U -> (io.A === io.B)
    ))

    when(io.Result === 0.U) {
        io.zero := 1.U
    }.otherwise {
        io.zero := 0.U
    }

    // sub = 1-2, if sub is 4 bit, sub = "1111", if sub is 5 bit, sub = "11111".

    val sum = io.A +& io.B
    // val sub = io.A -& io.B
    // printf("Sum: %d\n", sum) // Print the value of sum
    // printf("Subtraction: %d\n", sub) // Print the value of sub
    
    // carry used in unsigned, of used in signed.
    when(io.op === "b000".U) {
        io.carry    := sum(4)
        io.overflow := (io.A(3) === io.B(3)) && (io.Result(3) =/= io.A(3))
    }.elsewhen(io.op === "b001".U) {
        io.carry    := io.A < io.B
        io.overflow := (io.A(3) =/= io.B(3)) && (io.Result(3) =/= io.A(3))
    }
}

object alumain extends App {
    val rela_path: String = "lab3/alu/vsrc"
    println("generating the alu hardware")
    emitVerilog(new alu(), Array("--target-dir", Path.path + rela_path))
    // val a = new alu()
    // a.io.A = 1.U
}
