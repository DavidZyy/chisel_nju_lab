package rv32e.utils

import chisel3._
import chisel3.util._
import rv32e.config.Configs._

// class level 1
class SRAMBundleReadReq(val addrWidth: Int) extends Bundle {
    val raddr = Output(UInt(addrWidth.W))
}

class SRAMBundleReadResp(val dataWidth: Int) extends Bundle {
    val rdata = Output(UInt(dataWidth.W))
}

class SRAMBundleWriteReq(val addrWidth: Int, val dataWidth: Int) extends Bundle {
    val waddr = Output(UInt(addrWidth.W))
    val wdata = Output(UInt(dataWidth.W))
}

// class level 2
class SRAMReadBus(val addrWidth: Int, val dataWidth: Int) extends Bundle {
    val req  = Flipped(Decoupled(new SRAMBundleReadReq(addrWidth)))
    val resp = new SRAMBundleReadResp(dataWidth) 
}

class SRAMWriteBus(val addrWidth: Int, val dataWidth: Int) extends Bundle {
    val req = Flipped(Decoupled(new SRAMBundleWriteReq(addrWidth, dataWidth)))
}

// class level 3
/**
  * Sram template that can hold data, when pipeline block happens.
  *
  */
class SRAMTemplate(val addrWidth: Int, val dataWidth: Int, val Name: String) extends Module {
    val io = IO(new Bundle {
        val r = new SRAMReadBus(addrWidth, dataWidth)
        val w = new SRAMWriteBus(addrWidth, dataWidth)
    })

    io.r.req.ready := true.B
    io.w.req.ready := true.B
    val array = SyncReadMem(1<<addrWidth, UInt(dataWidth.W))
    val (ren, wen) = (io.r.req.valid, io.w.req.valid)
    val realRen = ren && !wen // assume is single port sram
    val rdata = array.read(io.r.req.bits.raddr, realRen)
    when(wen) (array(io.w.req.bits.waddr) := io.w.req.bits.wdata)

    io.r.resp.rdata := HoldUnless(rdata, RegNext(io.r.req.fire))

    Debug(RegNext(realRen), s"[SRAM][${Name}], raddr:%x, rdata:%x\n", io.r.req.bits.raddr, rdata) // use RegNet to wait for rdata
    Debug(wen, s"[SRAM][${Name}], waddr:%x, wdata:%x\n", io.w.req.bits.waddr, io.w.req.bits.wdata)
    assert(DATA_WIDTH == 32, "if cpu is 64 bits, this should be modify, in 32 bits cpu, both data and inst is 32 bits, but in " +
      "64 bit cpu, inst is 32 bits and data is 64 bits. So dataWidth can be 32 in icache and be 64 in dcache")
}

