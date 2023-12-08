package learn

import chisel3._
import chisel3.util._
// import chisel3.tester._
// import chisel3.tester.RawTester.test



// class SRAMTemplate[T <: Data](gen: T, set: Int, way: Int = 1,
//   shouldReset: Boolean = false, holdRead: Boolean = false, singlePort: Boolean = false) extends Module {
//   val io = IO(new Bundle {
//     val r = Flipped(new SRAMReadBus(gen, set, way))
//     val w = Flipped(new SRAMWriteBus(gen, set, way))
//   })
// 
//   val wordType = UInt(gen.getWidth.W)
//   val array = SyncReadMem(set, Vec(way, wordType))
//   val (resetState, resetSet) = (WireInit(false.B), WireInit(0.U))
// 
//   if (shouldReset) {
//     val _resetState = RegInit(true.B)
//     val (_resetSet, resetFinish) = Counter(_resetState, set)
//     when (resetFinish) { _resetState := false.B }
// 
//     resetState := _resetState
//     resetSet := _resetSet
//   }
// 
//   val (ren, wen) = (io.r.req.valid, io.w.req.valid || resetState)
//   val realRen = (if (singlePort) ren && !wen else ren)
// 
//   val setIdx = Mux(resetState, resetSet, io.w.req.bits.setIdx)
//   val wdataword = Mux(resetState, 0.U.asTypeOf(wordType), io.w.req.bits.data.asUInt)
//   val waymask = Mux(resetState, Fill(way, "b1".U), io.w.req.bits.waymask.getOrElse("b1".U))
//   val wdata = VecInit(Seq.fill(way)(wdataword))
//   when (wen) { array.write(setIdx, wdata, waymask.asBools) }
// 
//   val rdata = (if (holdRead) ReadAndHold(array, io.r.req.bits.setIdx, realRen)
//                else array.read(io.r.req.bits.setIdx, realRen)).map(_.asTypeOf(gen))
//   io.r.resp.data := VecInit(rdata)
// 
//   io.r.req.ready := !resetState && (if (singlePort) !wen else true.B)
//   io.w.req.ready := true.B
// 
//   Debug(false) {
//     when (wen) {
//       printf("%d: SRAMTemplate: write %x to idx = %d\n", GTimer(), wdata.asUInt, setIdx)
//     }
//     when (RegNext(realRen)) {
//       printf("%d: SRAMTemplate: read %x at idx = %d\n", GTimer(), VecInit(rdata).asUInt, RegNext(io.r.req.bits.setIdx))
//     }
//   }
// }