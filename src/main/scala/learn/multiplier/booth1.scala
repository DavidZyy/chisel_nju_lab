/**
  * boot 1 bit algorithm
  */
package learn.multiplier
import chisel3._
import chisel3.util._

// my version
// serial mutiplier use booth 1 bit algorithm
class booth1bit(bitWidth: Int) extends Module {
  val io = IO(new Bundle {
    val operandA = Input(SInt(bitWidth.W))
    val operandB = Input(SInt(bitWidth.W))
    val product = Output(SInt((2 * bitWidth).W))
  })

  val x = io.operandA
  val y = io.operandB
  
  val product = RegInit(0.U((2*bitWidth).W))

  val tempx = Cat(Fill(bitWidth, x(bitWidth - 1)), x)
  val x_reg = RegInit(UInt((2*bitWidth).W), tempx)
  val tempy = Cat(Fill(bitWidth-1, 0.U), y, 0.U(1.W))
  val y_reg = RegInit(tempy)

  // for (i <- 0 until bitWidth) {
    val rightmost2bits = Cat(y_reg(1), y_reg(0))
    val isAdd = rightmost2bits === "b01".U
    val isSub = rightmost2bits === "b10".U

    when(isSub) {
      product := product - x_reg
    }.elsewhen(isAdd) {
      product := product + x_reg
    }.otherwise {
      product := product
    }

    x_reg := x_reg << 1
    y_reg := y_reg >> 1
  // }


  io.product := product.asSInt
}

object booth1bit extends App {
  val path = "/home/zhuyangyang/project/nju_digital_design_chisel/all/booth1bit/vsrc"
  emitVerilog(new booth1bit(bitWidth=4), Array("--target-dir", path))
}
