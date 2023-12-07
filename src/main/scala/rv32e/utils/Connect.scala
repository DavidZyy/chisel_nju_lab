package rv32e.utils
import chisel3._
import chisel3.util._
import rv32e.bus.AXILiteIO_master
import rv32e.bus.AXILiteIO_slave
import rv32e.bus.AXIIO_master
import rv32e.bus.AXIIO_slave

// left -> right
object StageConnect {
    def apply[T <: Data](left: DecoupledIO[T], right: DecoupledIO[T]) = {
        // val fire = left.valid && right.ready
        // right.bits   :=  Mux(fire, left.bits, 0.U.asTypeOf(left.bits))

        right.bits   :=  left.bits
        right.valid  :=  left.valid
        left.ready   :=  right.ready
    }
}

object StageConnect_reg {
    def apply[T <: Data](left: DecoupledIO[T], right: DecoupledIO[T]) = {
        // val fire = left.valid && right.ready
        // right.bits   :=  Mux(fire, left.bits, 0.U.asTypeOf(left.bits))

        right.bits   :=  RegEnable(left.bits, left.valid && right.ready)
        right.valid  :=  left.valid
        left.ready   :=  right.ready
    }
}

object AxiLiteConnect {
    def apply(master: AXILiteIO_master, slave: AXILiteIO_slave) = {
        StageConnect(master.ar, slave.ar)
        StageConnect(slave.r, master.r)

        StageConnect(master.aw, slave.aw)
        StageConnect(master.w, slave.w)
        StageConnect(slave.b, master.b)
    }
}

object AxiConnect {
    def apply(master: AXIIO_master, slave: AXIIO_slave) = {
        StageConnect(master.ar, slave.ar)
        StageConnect(slave.r, master.r)

        StageConnect(master.aw, slave.aw)
        StageConnect(master.w, slave.w)
        StageConnect(slave.b, master.b)
    }
}