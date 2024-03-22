package rv32e.bus.simplebus

import chisel3._
import chisel3.util._

import rv32e.bus.axi4._

import rv32e.core.config._

class SimpleBus2AXI4Converter[OT <: AXI4Lite](outType: OT) extends Module with AXI4Parameters {
    val io = IO(new Bundle {
        val in  = Flipped(new SimpleBus)
        val out = Flipped(Flipped(outType)) // if get rid of two flipped, it will reported warning.
    })

    val (mem, axi) = (io.in, io.out)

    /**
      * NOTE: For axi-lite, aw and w can in one clock, but for axi burst mode, 
      * the first clock is aw, and the following clocks are w.
      */
    // for AXI4Lite
    axi.ar.valid      := mem.req.valid && mem.isARead
    axi.ar.bits.addr  := mem.req.bits.addr
    axi.r.ready       := mem.resp.ready
    axi.aw.valid      := mem.req.valid && mem.isAWrite
    axi.aw.bits.addr  := mem.req.bits.addr
    axi.w.valid       := mem.req.valid && mem.isWrite
    axi.w.bits.data   := mem.req.bits.wdata
    axi.w.bits.strb   := mem.req.bits.wmask
    axi.b.ready       := mem.resp.ready

    mem.req.ready     := MuxLookup(mem.req.bits.cmd, false.B)(List(
        SimpleBusCmd.aread       -> axi.ar.ready,
        SimpleBusCmd.burst_aread -> axi.ar.ready,

        SimpleBusCmd.awrite       -> axi.aw.ready,
        SimpleBusCmd.burst_awrite -> axi.aw.ready,

        SimpleBusCmd.write        -> axi.w.ready,
        SimpleBusCmd.write_burst  -> axi.w.ready,

        SimpleBusCmd.write_awrite -> (axi.w.ready && axi.aw.ready),
        SimpleBusCmd.write_burst_awrite -> (axi.w.ready && axi.aw.ready),
    ))
    // mem.req.ready       := when(mem.isARead) {
    //     axi.ar.ready
    // } .elsewhen (mem.isAWrite && mem.isWrite) {
    //     axi.aw.ready && axi.w.ready
    // } .elsewhen (mem.isAWrite) {
    //     axi.aw.ready
    // } .elsewhen (mem.isWrite) {
    //     axi.w.ready
    // } .otherwise { false.B}
    mem.resp.valid      := axi.r.valid || axi.b.valid

    mem.resp.bits.rdata := axi.r.bits.data
    mem.resp.bits.wresp := axi.b.bits.resp

    // the value of axi4 maybe optimized out in verilog for it is constant.
    if (outType.getClass == classOf[AXI4]) {
        val axi4 = io.out.asInstanceOf[AXI4]

        axi4.ar.bits.size  := DATA_WIDTH.U
        axi4.ar.bits.len   := mem.req.bits.len
        axi4.ar.bits.burst := BURST_INCR
        axi4.aw.bits.size  := DATA_WIDTH.U
        axi4.aw.bits.len   := mem.req.bits.len // if from cache to mem, if to device, the length is not this.
        axi4.aw.bits.burst := BURST_INCR
        axi4.w.bits.last   := mem.req.bits.wlast

        mem.resp.bits.rlast := axi4.r.bits.last
    } else {
        mem.resp.bits.rlast := true.B
    }
}

object SimpleBus2AXI4Converter {
    def apply[OT <: AXI4Lite](in: SimpleBus, outType: OT): OT = {
        val bridge = Module(new SimpleBus2AXI4Converter(outType))
        bridge.io.in <> in
        bridge.io.out
    }
}
