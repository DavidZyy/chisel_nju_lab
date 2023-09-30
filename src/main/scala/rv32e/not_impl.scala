package rv32e

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.config.Dec_Info._
import rv32e.config.Inst._

class NotImplBB extends BlackBox with HasBlackBoxResource {
    val io = IO(new Bundle{
        val not_impl = Input(Bool())
    })
    addResource("/NotImplBB.v")
}

class not_impl_moudle extends Module {
    val not_impl = IO(Input(Bool()))

    val NotImplBB_i1 = Module(new NotImplBB())

    NotImplBB_i1.io.not_impl := not_impl
}