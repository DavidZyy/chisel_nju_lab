package rv32e.bus

import chisel3._
import chisel3.util._
import rv32e.config.Configs._
import rv32e.config.Axi_Configs._
import rv32e.cache.HasCacheConst

class SimpleBus2AXI4Converter extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(new SimpleBus)
        val out = new AXI4
    })

    val (mem, axi) = (io.in, io.out)

    axi.ar.valid      := mem.req.valid && mem.isRead
    axi.ar.bits.addr  := mem.req.bits.addr
    axi.ar.bits.size  := DATA_WIDTH.U
    axi.ar.bits.len   := mem.req.bits.len
    axi.ar.bits.burst := INCR.U
    axi.r.ready       := mem.resp.ready
    axi.aw.valid      := mem.req.valid && mem.isAWrite
    axi.aw.bits.addr  := mem.req.bits.addr
    axi.aw.bits.size  := DATA_WIDTH.U
    axi.aw.bits.len   := mem.req.bits.len // if from cache to mem, if to device, the length is not this.
    axi.aw.bits.burst := INCR.U
    axi.w.valid       := mem.req.valid && mem.isWrite
    axi.w.bits.data   := mem.req.bits.wdata
    axi.w.bits.strb   := mem.req.bits.wmask
    axi.w.bits.last   := mem.req.bits.wlast
    axi.b.ready       := mem.resp.ready

    mem.req.ready     := MuxLookup(mem.req.bits.cmd, false.B)(List(
        SimpleBusCmd.read    -> axi.ar.ready,
        SimpleBusCmd.awrite  -> axi.aw.ready,
        SimpleBusCmd.write   -> axi.w.ready,
    ))
    mem.resp.valid      := axi.r.valid
    mem.resp.bits.rdata := axi.r.bits.data
    mem.resp.bits.wresp := axi.b.bits.resp
}

object SimpleBus2AXI4Converter {
    def apply(in: SimpleBus) = {
        val bridge = Module(new SimpleBus2AXI4Converter)
        bridge.io.in <> in
        bridge.io.out
    }
}
