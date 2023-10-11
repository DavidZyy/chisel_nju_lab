package rv32e

import chisel3._
import chisel3.util._

import rv32e.config.Configs._
import rv32e.define.Inst._
// import rv32e.bus.IFU2IDU_bus
// import rv32e.bus.EXU2IFU_bus
import rv32e.bus._

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
    val cur_pc          =   Output(UInt(ADDR_WIDTH.W))
}

class IFUIO extends Bundle {
    val in  = (new IFU_in_class)
    val out = (new IFU_out_class)
}

class IFU extends Module {
    val to_IDU   = IO(Decoupled(new IFU2IDU_bus)) // only to IDU signal
    val from_EXU = IO(Flipped(Decoupled(new EXU2IFU_bus)))

    val reg_PC  = RegInit(UInt(ADDR_WIDTH.W), START_ADDR.U)
    val next_PC = Wire(UInt(ADDR_WIDTH.W))
    from_EXU.ready := true.B
    to_IDU.valid   := true.B

    when (from_EXU.bits.bru_ctrl_br) {
        next_PC := from_EXU.bits.bru_addr
    } .elsewhen (from_EXU.bits.csr_ctrl_br) {
        next_PC := from_EXU.bits.csr_addr
    } .otherwise {
        next_PC := reg_PC + ADDR_BYTE_WIDTH.U
    }

    reg_PC := Mux(to_IDU.fire, next_PC, reg_PC)

    val RomBB_i1 = Module(new RomBB())
    RomBB_i1.io.addr    := reg_PC

    // if not ready, transfer nop inst
    to_IDU.bits.inst    := Mux(to_IDU.fire, RomBB_i1.io.inst, NOP.U)
    // to_IDU.bits.inst    := RomBB_i1.io.inst
    to_IDU.bits.pc      := reg_PC

    val s_idle :: s_wait_ready :: Nil = Enum(2)
    val state = RegInit(s_idle)
    state := MuxLookup(state, s_idle, List(
        s_idle          -> s_wait_ready,
        s_wait_ready    -> Mux(to_IDU.ready, s_idle, s_wait_ready)
    ))
    to_IDU.valid    :=  MuxLookup(state, false.B, List(
        s_idle       -> false.B,
        s_wait_ready -> true.B
    ))
}
