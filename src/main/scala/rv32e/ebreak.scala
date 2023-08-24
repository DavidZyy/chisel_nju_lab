package rv32e

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.config.Dec_Info._
import rv32e.config.Inst._

class EbreakBB extends BlackBox with HasBlackBoxResource {
    val io = IO(new Bundle{
        val is_ebreak = Input(Bool())
    })
    addResource("/EbreakBB.v")
}

class ebreak_moudle extends Module {
    val is_ebreak = IO(Input(Bool()))

    val EbreakBB_i1 = Module(new EbreakBB())

    EbreakBB_i1.io.is_ebreak := is_ebreak
}
