package rv32e.core.backend.fu

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._

import rv32e.core.config._
import rv32e.core.define.Dec_Info._

class NotImplBB extends BlackBox with HasBlackBoxResource {
    val io = IO(new Bundle{
        val clock    = Input(Clock())
        val valid = Input(Bool())
    })
    addResource("/NotImplBB.v")
}

class not_impl_moudle extends Module {
    val valid = IO(Input(Bool()))

    val NotImplBB_i1 = Module(new NotImplBB())

    NotImplBB_i1.io.clock    := clock
    NotImplBB_i1.io.valid := valid
}