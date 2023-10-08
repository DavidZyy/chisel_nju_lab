package rv32e

import chisel3._
import chisel3.util._

import rv32e.config.Configs._

import chisel3.util.experimental.loadMemoryFromFile
import firrtl.annotations.MemoryLoadFileType

class RomBB extends BlackBox with HasBlackBoxResource {
    val io = IO(new Bundle {
        val addr = Input(UInt(ADDR_WIDTH.W))
        val inst = Output(UInt(ADDR_WIDTH.W))
    })

    addResource("/RomBB.v")
}

class IFU_in_class extends Bundle {
    val ctrl_br         =   Input(Bool())   // come from bru result
    val addr_target     =   Input(UInt(ADDR_WIDTH.W)) // come from alu result
    val ctrl_csr        =   Input(Bool()) 
    val excpt_addr      =   Input(UInt(ADDR_WIDTH.W))
}

class IFU_out_class extends Bundle {
    val inst            =   Output(UInt(ADDR_WIDTH.W)) 
    val cur_pc          =   Output(UInt(ADDR_WIDTH.W))
}

class IFUIO extends Bundle {
    val in  = (new IFU_in_class)
    val out = (new IFU_out_class)
}

class IFU extends Module {
    val io = IO(new IFUIO())

    val RomBB_i1 = Module(new RomBB())

    val reg_PC = RegInit(UInt(ADDR_WIDTH.W), START_ADDR.U)
    when (io.in.ctrl_br) {
        reg_PC := io.in.addr_target
    } .elsewhen (io.in.ctrl_csr) {
        reg_PC := io.in.excpt_addr
    } .otherwise {
        reg_PC := reg_PC + ADDR_BYTE_WIDTH.U
    }

    RomBB_i1.io.addr := reg_PC

    io.out.inst   := RomBB_i1.io.inst
    io.out.cur_pc := reg_PC
}
