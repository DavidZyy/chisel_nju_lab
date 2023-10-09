package rv32e.utils
import chisel3._
import chisel3.util._

// left -> right
object StageConnect {
    def apply[T <: Data](left: DecoupledIO[T], right: DecoupledIO[T]) {
        right.bits   :=  left.bits
        right.valid  :=  left.valid
        left.ready   :=  right.ready
    } 
}
