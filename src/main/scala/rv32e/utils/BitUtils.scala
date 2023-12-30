package rv32e.utils

import chisel3._
import chisel3.util._

object MaskExpand {
 def apply(m: UInt) = Cat(m.asBools.map(Fill(8, _)).reverse)
}

object MaskData {

}
