package rv32e.utils
import chisel3._
import chisel3.util._

// left -> right
object StageConnect {
    def apply[T <: Data](left: DecoupledIO[T], right: DecoupledIO[T]) {
        // val fire = left.valid && right.ready
        // right.bits   :=  Mux(fire, left.bits, 0.U.asTypeOf(left.bits))

        right.bits   :=  left.bits
        right.valid  :=  left.valid
        left.ready   :=  right.ready
    } 
}

object StageConnect_reg {
    def apply[T <: Data](left: DecoupledIO[T], right: DecoupledIO[T]) {
        // val fire = left.valid && right.ready
        // right.bits   :=  Mux(fire, left.bits, 0.U.asTypeOf(left.bits))

        right.bits   :=  RegEnable(left.bits, left.valid && right.ready)
        right.valid  :=  left.valid
        left.ready   :=  right.ready
    } 
}