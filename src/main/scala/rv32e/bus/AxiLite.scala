package rv32e.bus
import chisel3._
import chisel3.util._

// generated by chatgpt
// AXI-Lite Interface
class AXILiteIO extends Bundle {
  val aw = Flipped(new DecoupledIO(new AXILiteWriteAddressChannel))
  val w = Flipped(new DecoupledIO(new AXILiteWriteDataChannel))
  val b = new DecoupledIO(new AXILiteWriteResponseChannel)
  val ar = Flipped(new DecoupledIO(new AXILiteReadAddressChannel))
  val r = new DecoupledIO(new AXILiteReadDataChannel)
}

// AXI-Lite Write Address Channel
class AXILiteWriteAddressChannel extends Bundle {
  val addr = UInt(32.W)
  val prot = UInt(3.W)
  val id = UInt(4.W)
}

// AXI-Lite Write Data Channel
class AXILiteWriteDataChannel extends Bundle {
  val data = UInt(32.W)
  val strb = UInt(4.W)
  val id = UInt(4.W)
}

// AXI-Lite Write Response Channel
class AXILiteWriteResponseChannel extends Bundle {
  val resp = UInt(2.W)
  val id = UInt(4.W)
}

// AXI-Lite Read Address Channel
class AXILiteReadAddressChannel extends Bundle {
  val addr = UInt(32.W)
  val prot = UInt(3.W)
  val id = UInt(4.W)
}

// AXI-Lite Read Data Channel
class AXILiteReadDataChannel extends Bundle {
  val data = UInt(32.W)
  val resp = UInt(2.W)
  val id = UInt(4.W)
}
