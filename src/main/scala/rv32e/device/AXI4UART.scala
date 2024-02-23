package rv32e.device

import chisel3._
import chisel3.util._

import rv32e.bus._
import rv32e.bus.axi4._
import rv32e.bus.simplebus._

import rv32e.utils._

import rv32e.core.config._

import chisel3.util.experimental.BoringUtils

class AXI4UART extends Module {
    val io = IO(new Bundle {
        val in = Flipped(new AXI4Lite)
    })

    val reg    = RegInit(0.U(DATA_WIDTH.W))

    val mapping = Map(
        RegMap(0x3f8, reg, null)
    )

    def getOffset(addr: UInt) = addr(11, 0)

    RegMap.generate(mapping, getOffset(io.in.ar.bits.addr), io.in.r.bits.data, 
    getOffset(io.in.aw.bits.addr), io.in.w.fire, io.in.w.bits.data, MaskExpand("b0001".U))

    io.in.ar.ready  := true.B
    io.in.r.valid   := true.B
    io.in.r.bits.resp := true.B
    io.in.aw.ready  := true.B
    io.in.w.ready   := true.B
    io.in.b.valid   := true.B
    io.in.b.bits.resp := true.B

    val EXUPC = Wire(UInt(ADDR_WIDTH.W))
    val EXUInst = Wire(UInt(ADDR_WIDTH.W))
    BoringUtils.addSink(EXUPC, "EXUPC")
    BoringUtils.addSink(EXUInst, "EXUInst")
    Debug(io.in.w.fire, "[uart], pc:%x, inst:%x, char:%c\n", EXUPC, EXUInst, io.in.w.bits.data(7, 0))
    // Debug(io.in.w.fire, "%c", reg(7, 0))
}