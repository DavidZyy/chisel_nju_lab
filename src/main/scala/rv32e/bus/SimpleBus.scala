package rv32e.bus

import chisel3._
import chisel3.util._
import rv32e.config.Configs._
import rv32e.config.Axi_Configs

object SimpleBusCmd {
    def read    = "b0000".U
    def write   = "b0001".U

    def apply() = UInt(4.W)
}

class SimpleBusReqBundle extends Bundle {
    val addr  = Output(UInt(ADDR_WIDTH.W))
    val len   = Output(UInt(Axi_Configs.AxLEN_WIDTH.W)) // the total entries - 1

    val wdata = Output(UInt(DATA_WIDTH.W))
    val wmask = Output(UInt(DATA_WIDTH.W))
    val wlast  = Output(Bool())

    val cmd   = Output(SimpleBusCmd())
    def isRead()  = (cmd === SimpleBusCmd.read)
    def isWrite() = (cmd === SimpleBusCmd.write)
}

class SimpleBusRespBundle extends Bundle {
    val rdata = Output(UInt(DATA_WIDTH.W))
    val wresp = Output(Bool())
}

class SimpleBus extends Bundle {
    val req  = Decoupled(new SimpleBusReqBundle)
    val resp = Flipped(Decoupled(new SimpleBusRespBundle))

    def isRead  = req.bits.isRead()
    def isWrite = req.bits.isWrite()
    def toAXI4() = SimpleBus2AXI4Converter(this)
}
