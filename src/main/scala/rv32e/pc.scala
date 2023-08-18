package rv32e

import chisel3._
import chisel3.util._

import rv32e.config.Configs._

class PCRegIO extends Bundle {
    val cur_pc          =   Output(UInt(ADDR_WIDTH.W))
    val ctrl_br         =   Input(Bool())   // come from bru result
    val addr_target     =   Input(UInt(ADDR_WIDTH.W)) // come from alu result
}

class PCReg extends Module {
    val io = IO(new PCRegIO())

    val regPC = RegInit(UInt(ADDR_WIDTH.W), START_ADDR.U)

    when (io.ctrl_br) {
        regPC := io.addr_target
    } .otherwise {
        regPC := regPC + ADDR_BYTE_WIDTH.U
    }

    io.cur_pc := regPC
}