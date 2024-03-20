// branch prediction module
package rv32e.core.frontend

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

import rv32e.bus._

import rv32e.core.config._

import rv32e.utils._

trait HasBPUConst {
    val setIdxWidth = 12 // 1 << 12 = 1024 entry
    val setLSB = 2
    val setMSB = setLSB + setIdxWidth - 1
}

// BTB use direct mapping
class BTBEntry extends Bundle {
    val valid  = Bool()
    val tag    = UInt(ADDR_WIDTH.W)
    val target = UInt(ADDR_WIDTH.W)
}

class BPU extends Module with HasBPUConst {
    val io = IO(new Bundle {
        val in  = new Bundle {
            val pc       = Flipped(Valid((UInt(ADDR_WIDTH.W))))
            val redirect = Flipped(new RedirectIO)
            val missPC   = Input(UInt(ADDR_WIDTH.W))
        }
        val out = new RedirectIO
    })

    val btb = Module(new SRAMTemplate(new BTBEntry, setIdxWidth, "BTB"))

    btb.io.r.req.valid      := io.in.pc.valid
    btb.io.r.req.bits.raddr := io.in.pc.bits(setMSB, setLSB)

    val btbRead = Wire(new BTBEntry)
    btbRead := btb.io.r.resp.rdata.asTypeOf(new BTBEntry)

    val pcLatch = RegEnable(io.in.pc.bits, io.in.pc.valid)
    val btbHit = btbRead.valid && (btbRead.tag === pcLatch) && !io.in.redirect.valid

    io.out.valid  := btbHit
    io.out.target := btbRead.target

    val btbWrite = WireInit(0.U.asTypeOf(new BTBEntry))
    btbWrite.valid  := true.B
    btbWrite.tag    := io.in.missPC // redirect pc
    btbWrite.target := io.in.redirect.target

    btb.io.w.req.valid      := io.in.redirect.valid
    btb.io.w.req.bits.waddr := io.in.missPC(setMSB, setLSB) // pc from exu setidx
    btb.io.w.req.bits.wdata := btbWrite.asUInt

    ////////////// for perf ///////////////
    if(EnablePerfCnt) {
        BoringUtils.addSource(io.in.pc.valid, perfPrefix+"BPUTime")
        BoringUtils.addSource(io.in.redirect.valid, perfPrefix+"BPUWrong")
    }
}

/**
  * to test the ipc improvement of BPU
  *
  */
class BPUdummy extends Module with HasBPUConst {
    val io = IO(new Bundle {
        val in  = new Bundle {
            val pc       = Flipped(Valid((UInt(ADDR_WIDTH.W))))
            val redirect = Flipped(new RedirectIO)
            val missPC   = Input(UInt(ADDR_WIDTH.W))
        }
        val out = new RedirectIO
    })

    io.out <> DontCare
}