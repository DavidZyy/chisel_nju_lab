package rv32e.utils

import chisel3._
import chisel3.util._

object RegMap {
    def apply(addr: Int, reg: UInt, wfn: UInt => UInt = (x => x)) = (addr, (reg, wfn))
    def generate(mapping: Map[Int, (UInt, UInt => UInt)], raddr: UInt, rdata: UInt,
        waddr: UInt, wen: Bool, wdata: UInt, wmask: UInt):Unit = {
        val chiselMapping = mapping.map { case (a, (r, w)) => (a.U, r, w)}
        rdata := LookupTree(raddr, chiselMapping.map { case (a, r, w) => (a, r)})
        chiselMapping.map { case (a, r, w) =>
            if (w != null) when (wen && waddr === a) {r := w(MaskData(r, wdata, wmask))}     
        }
    }
}
