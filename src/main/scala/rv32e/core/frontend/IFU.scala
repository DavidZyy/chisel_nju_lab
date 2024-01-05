package rv32e.core.frontend

import chisel3._
import chisel3.util._

import rv32e.bus._
import rv32e.bus.simplebus._

import rv32e.core.config._
import rv32e.core.define.Dec_Info._
import rv32e.core.define.Mem._
import rv32e.core.backend.fu._

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

class IFU_pipeline extends Module {
    val to_IDU     = IO(Decoupled(new IFU2IDU_bus)) // only to IDU signal
    val from_WBU   = IO(Flipped(Decoupled(new WBU2IFU_bus)))
    val to_mem     = IO(new SimpleBus)
    val fetch_PC   = IO(Output(UInt(ADDR_WIDTH.W)))
    val to_IDU_PC  = IO(Input(UInt(ADDR_WIDTH.W))) // from icache

    val reg_PC   = RegInit(UInt(ADDR_WIDTH.W), START_ADDR.U)

    val pcUpdate = to_mem.req.fire || from_WBU.bits.redirect.valid

    when(pcUpdate) {
        reg_PC := Mux(from_WBU.bits.redirect.valid, from_WBU.bits.redirect.target, reg_PC + ADDR_BYTE.U)
    }

    // to mem signals
    to_mem.req.valid      := to_IDU.ready
    to_mem.req.bits.addr  := reg_PC
    to_mem.req.bits.cmd   := SimpleBusCmd.read
    to_mem.req.bits.wdata := DontCare
    to_mem.req.bits.wmask := DontCare
    to_mem.req.bits.len   := 0.U
    to_mem.req.bits.wlast := true.B
    to_mem.resp.ready     := to_IDU.ready

    // to IDU signals
    to_IDU.valid     := to_mem.resp.valid
    to_IDU.bits.inst := to_mem.resp.bits.rdata
    to_IDU.bits.pc   := to_IDU_PC

    // from EXU signals
    from_WBU.ready  := true.B // always true.

    fetch_PC        := reg_PC
}

