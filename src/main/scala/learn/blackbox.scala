package learn

import chisel3._
import chisel3.util.HasBlackBoxInline
class BlackBoxRealAdd extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val in1 = Input(UInt(64.W))
    val in2 = Input(UInt(64.W))
    val out = Output(UInt(64.W))
  })
  setInline("BlackBoxRealAdd.v",
    """module BlackBoxRealAdd(
      |    input  [15:0] in1,
      |    input  [15:0] in2,
      |    output [15:0] out
      |);
      |always @* begin
      |  out <= $realtobits($bitstoreal(in1) + $bitstoreal(in2));
      |end
      |endmodule
    """.stripMargin)
}

class black_add extends Module {
    val io = IO(new Bundle {
        val in1 = Input(UInt(64.W))
        val in2 = Input(UInt(64.W))
        val out = Output(UInt(64.W))
    })

    val kan = Module(new BlackBoxRealAdd())    
    kan.io.in1  := io.in1
    kan.io.in2  := io.in2
    io.out  := kan.io.out
}

object black_main extends App {
    emitVerilog(new black_add(), Array("--target-dir", "generated/black_inline"))
}
