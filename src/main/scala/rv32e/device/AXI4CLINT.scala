package rv32e.device

import chisel3._
import chisel3.util._

import rv32e.bus._
import rv32e.bus.axi4._
import rv32e.bus.simplebus._

import rv32e.utils._

import rv32e.core.config._

import _root_.circt.stage.ChiselStage
import _root_.circt.stage.CIRCTTargetAnnotation
import _root_.circt.stage.CIRCTTarget
import _root_.circt.stage.FirtoolOption

import chisel3.util.experimental.BoringUtils

class AXI4CLINT extends Module {
    val io = IO(new Bundle {
        val in = Flipped(new AXI4Lite)
    })

    val mtime = RegInit(0.U((2*DATA_WIDTH).W))
    mtime := mtime + 1.U

    val mapping = Map(
        RegMap(0x48, mtime(DATA_WIDTH-1, 0), null),
        RegMap(0x4c, mtime(2*DATA_WIDTH-1, DATA_WIDTH), null)
    )

    def getOffset(addr: UInt) = addr(7,0)

    RegMap.generate(mapping, getOffset(io.in.ar.bits.addr), io.in.r.bits.data, 
    getOffset(io.in.aw.bits.addr), io.in.w.fire, io.in.w.bits.data, MaskExpand(io.in.w.bits.strb))

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
    // Debug(io.in.ar.fire, "[clint], pc:%x, inst:%x\n", EXUPC, EXUInst)
}

object myMultipierMain extends App {
  val path = "/home/zhuyangyang/project/CPU/chisel-empty/generated"
  val firtoolOptions = Seq (
      FirtoolOption("--disable-all-randomization"),
      FirtoolOption("--lowering-options=disallowLocalVariables, locationInfoStyle=none"),
      // FirtoolOption("--lowering-options=locationInfoStyle=none")
  )
  emitVerilog(new AXI4CLINT(), Array("--target-dir", path), firtoolOptions)
}
