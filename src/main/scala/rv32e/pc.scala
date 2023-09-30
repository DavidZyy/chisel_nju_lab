package rv32e

import chisel3._
import chisel3.util._

import rv32e.config.Configs._

class pc_in_class extends Bundle {
    val ctrl_br         =   Input(Bool())   // come from bru result
    val addr_target     =   Input(UInt(ADDR_WIDTH.W)) // come from alu result
    val ctrl_csr        =   Input(Bool()) 
    val excpt_addr      =   Input(UInt(ADDR_WIDTH.W))
}

class pc_out_class extends Bundle {
    val cur_pc          =   Output(UInt(ADDR_WIDTH.W))
}

class PCRegIO extends Bundle {
    val in  = (new pc_in_class)
    val out = (new pc_out_class)
}

class PCReg extends Module {
    val io = IO(new PCRegIO())

    val regPC = RegInit(UInt(ADDR_WIDTH.W), START_ADDR.U)

    when (io.in.ctrl_br) {
        regPC := io.in.addr_target
    } .elsewhen (io.in.ctrl_csr) {
        regPC := io.in.excpt_addr
    } .otherwise {
        regPC := regPC + ADDR_BYTE_WIDTH.U
    }

    io.out.cur_pc := regPC
}