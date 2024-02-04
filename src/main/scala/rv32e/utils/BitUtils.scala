package rv32e.utils

import chisel3._
import chisel3.util._

object MaskExpand {
    def apply(m: UInt) = Cat(m.asBools.map(Fill(8, _)).reverse)
}

// newData have be shift left according to the off in a word
object MaskData {
    def apply(oldData: UInt, newData: UInt, fullmask: UInt) = {
        require(oldData.getWidth == newData.getWidth)
        require(oldData.getWidth == fullmask.getWidth)
        (newData & fullmask) | (oldData & ~fullmask)
    }
}

object SignExt {
  def apply(a: UInt, len: Int) = {
    val aLen = a.getWidth
    val signBit = a(aLen-1)
    if (aLen >= len) a(len-1,0) else Cat(Fill(len - aLen, signBit), a)
  }
}

object ZeroExt {
  def apply(a: UInt, len: Int) = {
    val aLen = a.getWidth
    if (aLen >= len) a(len-1,0) else Cat(0.U((len - aLen).W), a)
  }
}
