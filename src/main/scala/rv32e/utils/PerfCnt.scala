package rv32e.utils

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

import rv32e.core.config._


class perfCnt extends Module {
    val perfCntList = Map(
        // bpu
        "BPUTime"  -> (0x00, perfPrefix+"BPUTime"),
        "BPUWrong" -> (0x01, perfPrefix+"BPUWrong"),
        // cache
        "icacheTime" -> (0x02, perfPrefix+"icacheTime"),
        "icacheMiss" -> (0x03, perfPrefix+"icacheMiss"),
        "dcacheTime" -> (0x04, perfPrefix+"dcacheTime"),
        "dcacheMiss" -> (0x05, perfPrefix+"dcacheMiss"),
        // exu
        "nrLSU" -> (0x06, perfPrefix+"nrLSU"),
        "nrBRU" -> (0x07, perfPrefix+"nrBRU"),
        "nrCSR" -> (0x08, perfPrefix+"nrCSR"),
        "nrALU" -> (0x09, perfPrefix+"nrALU"),
        "nrMDU" -> (0x0a, perfPrefix+"nrMDU"),
    )

    val nrPerfCnts = perfCntList.size
    val perCntCond = List.fill(nrPerfCnts)(WireInit(false.B))
    val perfCnts   = List.fill(nrPerfCnts)(RegInit(0.U(PerfRegWidth.W)))

    (perfCnts zip perCntCond).map { case (c, e) => { when(e) { c := c + 1.U}}}

    perfCntList.map { case (name, (addr, boringId)) =>
        BoringUtils.addSink(perCntCond(addr), boringId)
    }

    // output perfInfo when ebreak
    val ebreak = WireInit(false.B)
    BoringUtils.addSink(ebreak, "BoringEbreak")

    perfCntList.toSeq.sortBy(_._2._1).map { case (name, (addr, boringId)) =>
        Debug(ebreak, name + ": %d\n", perfCnts(addr))
    }

    def CalAccuracy(Total: String, Right: String, Wrong: String): UInt = {
        if (Right != "") {
            (perfCnts(perfCntList(Right)._1) * 100.U / perfCnts(perfCntList(Total)._1 ))
        } else if (Wrong != "") {
            ((perfCnts(perfCntList(Total)._1) - perfCnts(perfCntList(Wrong)._1)) * 100.U / perfCnts(perfCntList(Total)._1 ))
        } else {
            0.U
        }
    }

    Debug(ebreak, "BPU accuracy: %d%%\n",    CalAccuracy("BPUTime", "", "BPUWrong"))
    Debug(ebreak, "icache accuracy: %d%%\n", CalAccuracy("icacheTime", "", "icacheMiss"))
    Debug(ebreak, "dcache accuracy: %d%%\n", CalAccuracy("dcacheTime", "", "dcacheMiss"))
}