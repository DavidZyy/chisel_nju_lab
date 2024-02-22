package rv32e.bus.simplebus

import chisel3._
import chisel3.util._

import rv32e.bus.config._
import rv32e.bus.axi4._

import rv32e.core.config._

object SimpleBusCmd {
    def read    = "b0001".U
    def write   = "b0010".U // for axi w
    def awrite  = "b0100".U // for axi aw

    def apply() = UInt(4.W)
}

class SimpleBusReqBundle extends Bundle {
    val addr  = Output(UInt(ADDR_WIDTH.W))
    val len   = Output(UInt(lenWidth.W)) // the total entries - 1

    val wdata  = Output(UInt(DATA_WIDTH.W))
    val wmask  = Output(UInt((DATA_WIDTH/8).W))
    val wlast  = Output(Bool())

    val cmd   = Output(SimpleBusCmd())
    def isRead()   = (cmd === SimpleBusCmd.read)
    def isWrite()  = (cmd === SimpleBusCmd.write)
    def isAWrite() = (cmd === SimpleBusCmd.awrite)
}

class SimpleBusRespBundle extends Bundle {
    val rdata = Output(UInt(DATA_WIDTH.W))
    val wresp = Output(Bool())
}

class SimpleBus extends Bundle {
    val req  = Decoupled(new SimpleBusReqBundle)
    val resp = Flipped(Decoupled(new SimpleBusRespBundle))

    def isRead   = req.bits.isRead()
    def isWrite  = req.bits.isWrite()
    def isAWrite = req.bits.isAWrite()
    // def toAXI4() = SimpleBus2AXI4Converter(this)
    def toAXI4Lite() = SimpleBus2AXI4Converter(this, new AXI4Lite)
    def toAXI4()     = SimpleBus2AXI4Converter(this, new AXI4)
}
