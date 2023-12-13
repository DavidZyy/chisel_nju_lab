package rv32e.utils

import chisel3._
import chisel3.util._

/**
  * flush is used to invalid the current data in regs, rightOutFire is used to invalid the old data in regs that has been used.
  * so if flush, we can put all 0 in regs.
  */
object PipelineConnect {
  def apply[T <: Data](left: DecoupledIO[T], right: DecoupledIO[T], rightOutFire: Bool, isFlush: Bool) = {
    val valid = RegInit(false.B)
    when (rightOutFire) { valid := false.B } // the data in reg is be used, so set it to false
    when (left.valid && right.ready) { valid := true.B } // new data in, so set it to true
    when (isFlush) { valid := false.B } // data is be flushed, so set it false

    val ze = 0.U.asTypeOf(left.bits)
    left.ready := right.ready
    right.bits := RegEnable(Mux(isFlush, ze, left.bits), left.valid && right.ready)
    // right.bits := RegEnable(left.bits, left.valid && right.ready)
    right.valid := valid //&& !isFlush
  }
}