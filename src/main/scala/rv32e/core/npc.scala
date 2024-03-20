package rv32e.core

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import chisel3.util.experimental.BoringUtils
import chisel3.stage._

import _root_.circt.stage.ChiselStage
import _root_.circt.stage.CIRCTTargetAnnotation
import _root_.circt.stage.CIRCTTarget
import _root_.circt.stage.FirtoolOption

import rv32e.bus._
import rv32e.bus.simplebus._
import rv32e.bus.axi4._

import rv32e.core.backend._
import rv32e.core.frontend._
import rv32e.core.define.Mem._
import rv32e.core.config._
import rv32e.core.mem.cache._

import rv32e.device._

import rv32e.utils._

// npc module
class npc extends Module {
    val io = IO(new Bundle {
        val master = (new AXI4)
    })

    val core_i = Module(new core())
    
}
