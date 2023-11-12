package rv32e

import chisel3._
import chisel3.util._

import rv32e.config.Configs._
import rv32e.define.Inst._
import rv32e.bus._

import chisel3.util.experimental.loadMemoryFromFile
import firrtl.annotations.MemoryLoadFileType

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
    val from_WBU = IO(Flipped(Decoupled(new WBU2IFU_bus)))
    val axi      = IO(new AXILiteIO_master)
    // val to_Cache = IO(Decoupled(new IFU2Cache_bus))
    // val from_Cache = IO(Flipped(Decoupled(new Cache2IFU_bus)))

    val reg_PC  = RegInit(UInt(ADDR_WIDTH.W), START_ADDR.U)
    val next_PC = Wire(UInt(ADDR_WIDTH.W))

    when (from_EXU.bits.bru_ctrl_br) {
        next_PC := from_EXU.bits.bru_addr
    } .elsewhen (from_EXU.bits.csr_ctrl_br) {
        next_PC := from_EXU.bits.csr_addr
    } .otherwise {
        next_PC := reg_PC + ADDR_BYTE_WIDTH.U
    }

    // in some cycle, it has some instruction issue
    reg_PC := Mux(from_WBU.fire, next_PC, reg_PC)

    // state machine
    val s_idle :: s_wait_data :: s_wait_WB :: Nil = Enum(3)
    val state = RegInit(s_idle)
    state := MuxLookup(state, s_idle, List(
        s_idle      ->  Mux(axi.ar.fire,   s_wait_data,    s_idle),
        s_wait_data ->  Mux(axi.r.fire,    s_wait_WB,      s_wait_data),
        s_wait_WB   ->  Mux(from_WBU.fire, s_idle,         s_wait_WB)
    ))

    // axi master signals
    axi.ar.valid     := MuxLookup(state, false.B, List( s_idle      ->  true.B))
    axi.ar.bits.addr := reg_PC
    axi.r.ready      := MuxLookup(state, false.B, List( s_wait_data ->  true.B))
    axi.aw.valid     := false.B
    axi.aw.bits.addr := 0.U
    axi.w.valid      := false.B
    axi.w.bits.data  := 0.U
    axi.w.bits.strb  := 0.U
    axi.b.ready      := false.B

    // to IDU signals
    to_IDU.valid     := MuxLookup(state, false.B, List( s_wait_WB   ->  true.B))
    to_IDU.bits.inst := Mux(to_IDU.fire, axi.r.bits.data, NOP.U) // if not ready, transfer nop inst
    to_IDU.bits.pc   := reg_PC

    // from EXU signals
    from_EXU.ready  := MuxLookup(state, false.B, List( s_wait_WB   ->  true.B))

    // from WBU signals
    from_WBU.ready  := MuxLookup(state, false.B, List( s_wait_WB   ->  true.B))
}
