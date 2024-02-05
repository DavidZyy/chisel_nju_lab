package learn.divider

import chisel3._
import chisel3.util._

import rv32e.utils._

import _root_.circt.stage.ChiselStage
import _root_.circt.stage.CIRCTTargetAnnotation
import _root_.circt.stage.CIRCTTarget
import _root_.circt.stage.FirtoolOption

/**
  * my shift method: Cast both dividend and divisor to 2*bitWidth, and quotient is bitWidth.
  * For each cycle, shift right 1 bit divisor, and shift left 1 bit quotient.
  * 1 cycle get 1 bit of quotient.
  * 
  * other method: dividend is 2*bitWidth, divisor is bitWidth, for each cycle, dividend shift 
  * left 1 bit and select the top (bitWidth+1) of it.
  * @param bitWidth
  */
class divider0(bitWidth: Int) extends Module {
    val io = IO(new Bundle {
        val operandA = Input(UInt(bitWidth.W))
        val operandB = Input(UInt(bitWidth.W))
        val quotient = Output(UInt(bitWidth.W)) // quotient
        val remainder = Output(UInt(bitWidth.W)) // remainder

        val xReg = Output(UInt((2*bitWidth).W)) // for debug
        val yReg = Output(UInt((2*bitWidth).W)) // for debug
        val qReg = Output(UInt(bitWidth.W)) // for debug
    })

    val dividendNeg = io.operandA(bitWidth - 1).asBool
    val divisorNeg  = io.operandB(bitWidth - 1).asBool

    val x = Mux(dividendNeg, ~io.operandA+1.U, io.operandA)
    val y = Mux(divisorNeg, ~io.operandB+1.U, io.operandB) // typo divisorNeg to dividendNeg, bug here

    /* in our situation, reset signal means a division request is comme. */
    val xReg = RegInit(UInt((2*bitWidth).W), ZeroExt(x, 2*bitWidth))
    val yReg = RegInit(UInt((2*bitWidth).W), Cat(0.U(1.W), y, 0.U((bitWidth-1).W)))
    val qReg = RegInit(UInt((bitWidth).W), 0.U)

    val subtract = xReg - yReg
    val neg = subtract(2*bitWidth-1)
     
    xReg := Mux(neg, xReg, subtract)
    yReg := yReg >> 1
    qReg := Mux(neg, (qReg<<1)+0.U, (qReg<<1)+1.U) // last bit is 0.U or 1.U

    io.quotient := Mux((dividendNeg === divisorNeg), qReg, ~qReg+1.U)
    io.remainder := Mux(dividendNeg, ~xReg+1.U, xReg)

    io.xReg := xReg
    io.yReg := yReg
    io.qReg := qReg
}

object divider0Main extends App {
  val path = "/home/zhuyangyang/project/CPU/chisel-empty/generated"
  val firtoolOptions = Seq (
      FirtoolOption("--disable-all-randomization"),
      FirtoolOption("--lowering-options=disallowLocalVariables, locationInfoStyle=none"),
      // FirtoolOption("--lowering-options=locationInfoStyle=none")
  )
  emitVerilog(new divider0(bitWidth = 8), Array("--target-dir", path), firtoolOptions)
}
