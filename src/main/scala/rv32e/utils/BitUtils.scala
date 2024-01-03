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
