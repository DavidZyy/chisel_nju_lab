package rv32e.dev

// the codes here modified according to  
// https://ysyx.oscc.cc/slides/2205/18.html#/axi-lite%E6%8E%A5%E5%8F%A3%E7%9A%84rom
// A sram with axi-lite bus, it acts as a slave, and IFU is it's master.
// wrapper for DPI-C verilog, connected to IFU, LSU

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.define.Dec_Info._
import rv32e.define.Inst._
import rv32e.bus.AXILiteIO

class SRAM extends Module {
    val io = new AXILiteIO

    io.ar.fire
}
