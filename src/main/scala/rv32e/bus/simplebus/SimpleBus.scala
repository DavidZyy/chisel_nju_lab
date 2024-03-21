package rv32e.bus.simplebus

import chisel3._
import chisel3.util._

import rv32e.bus.config._
import rv32e.bus.axi4._

import rv32e.core.config._

// object SimpleBusCmd {
//     def read    = "b0001".U
//     def write   = "b0010".U // for axi w
//     def awrite  = "b0100".U // for axi aw
// 
//     def apply() = UInt(4.W)
// }

object SimpleBusCmd {
    // read | write | burst | awrite | aread, some bits can be true togeter, but some not.
    def idle         = "b00000"
    def aread        = "b00001".U // used for axi addr read channel and in core write, issue a read request
    def awrite       = "b00010".U // used for axi addr write channel and in core write, issue a write request
    def burst_aread  = "b00101".U // used for axi addr read channel with burst
    def burst_awrite = "b00110".U // used for axi addr write channel with burst

    def write        = "b01000".U  // used for axi write channel 
    def write_awrite = "b01010".U  // used for aw and w valid in one cycle
    def write_burst  = "b01100".U
    def write_burst_awrite = "b01110".U

    def read         = "b10000".U // used for axi read channel, maybe useless with the following one...
    def read_burst   = "b10100".U

    def apply() = UInt(5.W)
}

class SimpleBusReqBundle extends Bundle {
    // axi w channel
    val wdata  = Output(UInt(DATA_WIDTH.W))
    val wmask  = Output(UInt((DATA_WIDTH/8).W))
    val wlast  = Output(Bool())

    // axi ar && aw channel
    val addr  = Output(UInt(ADDR_WIDTH.W))
    val len   = Output(UInt(lenWidth.W)) // the total entries - 1
    val cmd   = Output(SimpleBusCmd())

    def isARead()  = (cmd(0) === 1.U)
    def isAWrite() = (cmd(1) === 1.U)
    def isBurst()  = (cmd(2) === 1.U)
    def isWrite()  = (cmd(3) === 1.U)
}

class SimpleBusRespBundle extends Bundle {
    // axi r channel
    val rdata = Output(UInt(DATA_WIDTH.W))
    // val rlast = Output(Bool())

    // axi b channel
    val wresp = Output(Bool())
}

class SimpleBus extends Bundle {
    val req  = Decoupled(new SimpleBusReqBundle)
    val resp = Flipped(Decoupled(new SimpleBusRespBundle))

    def isARead  = req.bits.isARead()
    def isAWrite = req.bits.isAWrite()
    def isBurst  = req.bits.isBurst()
    def isWrite  = req.bits.isWrite()
    // def toAXI4() = SimpleBus2AXI4Converter(this)
    def toAXI4Lite() = SimpleBus2AXI4Converter(this, new AXI4Lite)
    def toAXI4()     = SimpleBus2AXI4Converter(this, new AXI4)
}
