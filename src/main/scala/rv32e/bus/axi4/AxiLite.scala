/**
  * Axi-lite protocol
  */
// package rv32e.bus.axi4
// import chisel3._
// import chisel3.util._
// import rv32e.config.Configs._
// import rv32e.config.Cache_Configs._
// import rv32e.config.Axi_Configs._
// 
// class AXILiteIO_slave extends Bundle {
//   val ar = Flipped(Decoupled(new AXILiteReadAddressChannel))
//   val r  = Decoupled(new AXILiteReadDataChannel)
//   val aw = Flipped(Decoupled(new AXILiteWriteAddressChannel))
//   val w  = Flipped(Decoupled(new AXILiteWriteDataChannel))
//   val b  = Decoupled(new AXILiteWriteResponseChannel)
// }
// 
// class AXILiteIO_master extends Bundle {
//   val ar = Decoupled(new AXILiteReadAddressChannel)
//   val r  = Flipped(Decoupled(new AXILiteReadDataChannel))
//   val aw = Decoupled(new AXILiteWriteAddressChannel)
//   val w  = Decoupled(new AXILiteWriteDataChannel)
//   val b  = Flipped(Decoupled(new AXILiteWriteResponseChannel))
// }
// 
// // AXI-Lite Read Address Channel
// class AXILiteReadAddressChannel extends Bundle {
//   val addr = UInt(ADDR_WIDTH.W)
// }
// 
// // AXI-Lite Read Data Channel
// class AXILiteReadDataChannel extends Bundle {
//   val data = UInt(DATA_WIDTH.W)
//   val resp = UInt(RRESP_WIDTH.W)
// }
// 
// // AXI-Lite Write Address Channel
// class AXILiteWriteAddressChannel extends Bundle {
//   val addr = UInt(ADDR_WIDTH.W)
// }
// 
// // AXI-Lite Write Data Channel
// class AXILiteWriteDataChannel extends Bundle {
//   val data = UInt(DATA_WIDTH.W)
//   val strb = UInt(WSTRB_WIDTH.W)
// }
// 
// // AXI-Lite Write Response Channel
// class AXILiteWriteResponseChannel extends Bundle {
//   val resp = UInt(BRESP_WIDTH.W)
// }
