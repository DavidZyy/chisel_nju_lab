/**
  * whole Axi protocol
  */
package rv32e.bus

import chisel3._
import chisel3.util._
import rv32e.config.Configs._
import rv32e.config.Cache_Configs._
import rv32e.config.Axi_Configs._

class AXIIO_slave extends Bundle {
  val ar = Flipped(Decoupled(new AXIReadAddressChannel))
  val r  = Decoupled(new AXIReadDataChannel)
  val aw = Flipped(Decoupled(new AXIWriteAddressChannel))
  val w  = Flipped(Decoupled(new AXIWriteDataChannel))
  val b  = Decoupled(new AXIWriteResponseChannel)
}

class AXIIO_master extends Bundle {
  val ar = Decoupled(new AXIReadAddressChannel)
  val r  = Flipped(Decoupled(new AXIReadDataChannel))
  val aw = Decoupled(new AXIWriteAddressChannel)
  val w  = Decoupled(new AXIWriteDataChannel)
  val b  = Flipped(Decoupled(new AXIWriteResponseChannel))
}

// AXI Read Address Channel
class AXIReadAddressChannel extends Bundle {
  val addr  = UInt(ADDR_WIDTH.W)
  val size  = UInt(AxSIZE_WIDTH.W)
  val len   = UInt(AxLEN_WIDTH.W)
  val burst = UInt(AxBURST_WIDTH.W)
}

// AXI Read Data Channel
class AXIReadDataChannel extends Bundle {
  val data = UInt(DATA_WIDTH.W)
  val resp = UInt(RRESP_WIDTH.W) // 0 reprezents ok
  val last = Bool()
}

// AXI Write Address Channel
class AXIWriteAddressChannel extends Bundle {
  val addr  = UInt(ADDR_WIDTH.W)
  val size  = UInt(AxSIZE_WIDTH.W)
  val len   = UInt(AxLEN_WIDTH.W)
  val burst = UInt(AxBURST_WIDTH.W)
}

// AXI Write Data Channel
class AXIWriteDataChannel extends Bundle {
  val data = UInt(DATA_WIDTH.W)
  val strb = UInt(WSTRB_WIDTH.W)
  val last = Bool()
}

// AXI Write Response Channel
class AXIWriteResponseChannel extends Bundle {
  val resp = UInt(BRESP_WIDTH.W) // 0 reprezents ok
}
