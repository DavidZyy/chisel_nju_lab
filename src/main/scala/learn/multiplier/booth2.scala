package learn.multiplier
import chisel3._
import chisel3.util._

class booth2bit(bitWidth: Int) extends Module {
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
  
  val rightmost3bits = Cat(y_reg(2), y_reg(1), y_reg(0))
  val isAdd  = (rightmost3bits === "b001".U) || (rightmost3bits === "b010".U)
  val isSub  = (rightmost3bits === "b101".U) || (rightmost3bits === "b110".U)
  val isAdd2 = (rightmost3bits === "b011".U)
  val isSub2 = (rightmost3bits === "b100".U)

  val value = WireInit(0.U((2*bitWidth).W))

//   when(isSub) {
//     value := ~x_reg + 1.U
//   }.elsewhen(isAdd) {
//     value := x_reg
//   }.elsewhen(isSub2) {
//     value := ~(x_reg << 1) + 1.U
//   }.elsewhen(isAdd2) {
//     value := x_reg << 1
//   } .otherwise {
//     value := 0.U
//   }
// 
//   product := product + value

  when(isSub) {
    product := product - x_reg
  }.elsewhen(isAdd) {
    product := product + x_reg
  }.elsewhen(isSub2) {
    // product := product - x_reg << 1 //wrong
    product := product - (x_reg << 1) //right
  }.elsewhen(isAdd2) {
    product := product + (x_reg << 1)
  } .otherwise {
    product := product
  }

  x_reg := x_reg << 2
  y_reg := y_reg >> 2 // forget shift right 2

  io.product := product.asSInt()
}

object booth2bit extends App {
  val path = "/home/zhuyangyang/project/nju_digital_design_chisel/all/booth2bit/vsrc"
  emitVerilog(new booth2bit(bitWidth=4), Array("--target-dir", path))
}

