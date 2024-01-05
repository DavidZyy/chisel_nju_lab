/**
  * whole Axi protocol
  */
package rv32e.bus.axi4

import chisel3._
import chisel3.util._

import rv32e.bus.config._

import rv32e.core.config._

trait axiConf {
  val BRESP_WIDTH   = 1
  val RRESP_WIDTH   = 1
  val WSTRB_WIDTH   = DATA_BYTE
  val AxSIZE_WIDTH  = 3 // AWSIZE and ARSIZE
  val AxLEN_WIDTH   = lenWidth
  val AxBURST_WIDTH = 2
  val FIXED = 0
  val INCR  = 1
  val WRAP  = 2
}

class AXI4 extends Bundle {
  val ar = Decoupled(new AXIReadAddressChannel)
  val r  = Flipped(Decoupled(new AXIReadDataChannel))
  val aw = Decoupled(new AXIWriteAddressChannel)
  val w  = Decoupled(new AXIWriteDataChannel)
  val b  = Flipped(Decoupled(new AXIWriteResponseChannel))
}

// AXI Read Address Channel
class AXIReadAddressChannel extends Bundle with axiConf{
  val addr  = UInt(ADDR_WIDTH.W)
  val size  = UInt(AxSIZE_WIDTH.W)
  val len   = UInt(AxLEN_WIDTH.W)
  val burst = UInt(AxBURST_WIDTH.W)
}

// AXI Read Data Channel
class AXIReadDataChannel extends Bundle with axiConf{
  val data = UInt(DATA_WIDTH.W)
  val resp = UInt(RRESP_WIDTH.W) // 0 reprezents ok
  val last = Bool()
}

// AXI Write Address Channel
class AXIWriteAddressChannel extends Bundle with axiConf{
  val addr  = UInt(ADDR_WIDTH.W)
  val size  = UInt(AxSIZE_WIDTH.W)
  val len   = UInt(AxLEN_WIDTH.W)
  val burst = UInt(AxBURST_WIDTH.W)
}

// AXI Write Data Channel
class AXIWriteDataChannel extends Bundle with axiConf{
  val data = UInt(DATA_WIDTH.W)
  val strb = UInt(WSTRB_WIDTH.W)
  val last = Bool()
}

// AXI Write Response Channel
class AXIWriteResponseChannel extends Bundle with axiConf{
  val resp = UInt(BRESP_WIDTH.W) // 0 reprezents ok
}
