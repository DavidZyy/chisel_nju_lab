package rv32e.fu

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.define.Dec_Info._
import rv32e.define.Inst._

class EbreakBB extends BlackBox with HasBlackBoxResource {
    val io = IO(new Bundle{
        val clock     = Input(Clock())
        val valid = Input(Bool())
    })
    addResource("/EbreakBB.v")
}

class ebreak_moudle extends Module {
    val valid = IO(Input(Bool()))

    val EbreakBB_i1 = Module(new EbreakBB())

    EbreakBB_i1.io.clock     := clock
    EbreakBB_i1.io.valid := valid
}
