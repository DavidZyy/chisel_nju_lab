package rv32e.utils

import chisel3._
import chisel3.util._

object GTimer {
  def apply() = {
    val c = RegInit(4.U(32.W))
    c := c + 2.U              // gtkwave's one cycle use two seconds, for debug conveniently, add 2.
    c
  }
}
