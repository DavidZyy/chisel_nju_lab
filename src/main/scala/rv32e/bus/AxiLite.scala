package rv32e.bus
import chisel3._
import chisel3.util._


class AXILiteIO_slave extends Bundle {
  val aw = Flipped(Decoupled(new AXILiteWriteAddressChannel))
  val w  = Flipped(Decoupled(new AXILiteWriteDataChannel))
  val b  = Decoupled(new AXILiteWriteResponseChannel)
  val ar = Flipped(Decoupled(new AXILiteReadAddressChannel))
  val r  = Decoupled(new AXILiteReadDataChannel)
}

class AXILiteIO_master extends Bundle {
  val aw = Decoupled(new AXILiteWriteAddressChannel)
  val w  = Decoupled(new AXILiteWriteDataChannel)
  val b  = Flipped(Decoupled(new AXILiteWriteResponseChannel))
  val ar = Decoupled(new AXILiteReadAddressChannel)
  val r  = Flipped(Decoupled(new AXILiteReadDataChannel))
}

// AXI-Lite Write Address Channel
class AXILiteWriteAddressChannel extends Bundle {
  val addr = UInt(32.W)
  // val prot = UInt(3.W)
  // val id = UInt(4.W)
}

// AXI-Lite Write Data Channel
class AXILiteWriteDataChannel extends Bundle {
  val data = UInt(32.W)
  val strb = UInt(4.W)
  // val id = UInt(4.W)
}

// AXI-Lite Write Response Channel
class AXILiteWriteResponseChannel extends Bundle {
  // val resp = UInt(2.W)
  // val id = UInt(4.W)
}

// AXI-Lite Read Address Channel
class AXILiteReadAddressChannel extends Bundle {
  val addr = UInt(32.W)
  // val prot = UInt(3.W)
  // val id = UInt(4.W)
}

// AXI-Lite Read Data Channel
class AXILiteReadDataChannel extends Bundle {
  val data = UInt(32.W)
  // val resp = UInt(2.W)
  // val id = UInt(4.W)
}
