package rv32e.bus

import chisel3._
import chisel3.util._
import rv32e.config.Configs._

object SimpleBusCmd {
    def read    = "b0000".U
    def write   = "b0001".U

    def apply() = UInt(4.W)
}

class SimpleBusReqBundle extends Bundle {
    val addr  = Output(UInt(ADDR_WIDTH.W))

    val wdata = Output(UInt(DATA_WIDTH.W))
    val cmd   = Output(SimpleBusCmd())
    val wmask = Output(UInt(DATA_WIDTH.W))

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
}
