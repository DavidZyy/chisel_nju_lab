package rv32e.device

import chisel3._
import chisel3.util._

import rv32e.bus._
import rv32e.bus.axi4._
import rv32e.bus.simplebus._

import rv32e.core.define.Mem._

import rv32e.device._

// class MMIO extends Module {
//     val from_lsu = IO(Flipped(new SimpleBus))
// 
//     val RamBB_i1 = Module(new RamBB())
//     RamBB_i1.io.clock   := clock
//     RamBB_i1.io.addr    := from_lsu.req.bits.addr
//     RamBB_i1.io.mem_wen := from_lsu.isWrite
//     RamBB_i1.io.valid   := from_lsu.req.valid
//     RamBB_i1.io.wdata   := from_lsu.req.bits.wdata
//     RamBB_i1.io.wmask   := from_lsu.req.bits.wmask
// 
//     // req
//     from_lsu.req.ready  := true.B
// 
//     // resp
//     from_lsu.resp.valid := true.B
//     from_lsu.resp.bits.rdata := RamBB_i1.io.rdata
//     from_lsu.resp.bits.wresp := from_lsu.isWrite
// }

class MMIO extends Module {
    val io = IO(new Bundle {
        val in = Flipped(new SimpleBus)
        val flush = Input(Bool())
    })

    val addrSpace = List(
        (RTC_ADDR+8L, SERIAL_PORT-RTC_ADDR-8L),
        (SERIAL_PORT, 4L),
        (SERIAL_PORT+4L, mmioSize),
    )

    val mmioXbar = Module(new SimpleBusCrossBar1toN(addrSpace))

    val RamBB_i  = Module(new RamBB())
    val uart     = Module(new AXI4UART())
    val RamBB_i1 = Module(new RamBB())

    io.in <> mmioXbar.io.in
    mmioXbar.io.flush := io.flush

    RamBB_i.io.clock   := clock
    RamBB_i.io.addr    := mmioXbar.io.out(0).req.bits.addr
    RamBB_i.io.mem_wen := mmioXbar.io.out(0).isWrite
    RamBB_i.io.valid   := mmioXbar.io.out(0).req.valid
    RamBB_i.io.wdata   := mmioXbar.io.out(0).req.bits.wdata
    RamBB_i.io.wmask   := mmioXbar.io.out(0).req.bits.wmask
    mmioXbar.io.out(0).req.ready  := true.B
    mmioXbar.io.out(0).resp.valid := true.B
    mmioXbar.io.out(0).resp.bits.rdata := RamBB_i.io.rdata
    mmioXbar.io.out(0).resp.bits.rlast := true.B
    mmioXbar.io.out(0).resp.bits.wresp := true.B

    mmioXbar.io.out(1).toAXI4Lite()  <>  uart.io.in

    RamBB_i1.io.clock   := clock
    RamBB_i1.io.addr    := mmioXbar.io.out(2).req.bits.addr
    RamBB_i1.io.mem_wen := mmioXbar.io.out(2).isWrite
    RamBB_i1.io.valid   := mmioXbar.io.out(2).req.valid
    RamBB_i1.io.wdata   := mmioXbar.io.out(2).req.bits.wdata
    RamBB_i1.io.wmask   := mmioXbar.io.out(2).req.bits.wmask
    mmioXbar.io.out(2).req.ready  := true.B
    mmioXbar.io.out(2).resp.valid := true.B
    mmioXbar.io.out(2).resp.bits.rdata := RamBB_i1.io.rdata
    mmioXbar.io.out(2).resp.bits.rlast := true.B
    mmioXbar.io.out(2).resp.bits.wresp := true.B
}