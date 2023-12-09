package rv32e.device

import chisel3._
import chisel3.util._
import rv32e.bus._

class MMIO extends Module {
    val from_lsu = IO(Flipped(new SimpleBus))

    val RamBB_i1 = Module(new RamBB())
    RamBB_i1.io.clock   := clock
    RamBB_i1.io.addr    := from_lsu.req.bits.addr
    RamBB_i1.io.mem_wen := from_lsu.isWrite
    RamBB_i1.io.valid   := from_lsu.req.valid
    RamBB_i1.io.wdata   := from_lsu.req.bits.wdata
    RamBB_i1.io.wmask   := from_lsu.req.bits.wmask

    // req
    from_lsu.req.ready  := true.B

    // resp
    from_lsu.resp.valid := true.B
    from_lsu.resp.bits.rdata := RamBB_i1.io.rdata
    from_lsu.resp.bits.wresp := from_lsu.isWrite
}
