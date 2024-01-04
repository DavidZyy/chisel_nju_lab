package rv32e.bus

import chisel3._
import chisel3.util._
 import os.list
 
/** 
  * USAGE:
  * lsu  -> dcache & mmio
  * mmio -> devs
  * @param addressSpace
  */
// class SimpleBusCrossBar1toN(addressSpace: List[(Long, Long)]) extends Module {
//     val io = IO(new Bundle {
//         val in  = Flipped(new SimpleBus)
//         val out = Vec(addressSpace.length, new SimpleBus)
//         val flush = Input(Bool())
//     })
// 
//     val s_idle :: s_resp :: s_error :: Nil = Enum(3)
//     val state = RegInit(s_idle)
// 
//     // select the output channel according to the address
//     val addr = io.in.req.bits.addr
//     val outMatchVec = VecInit(addressSpace.map(
//       range => (addr >= range._1.U && addr < (range._1 + range._2).U)))
//     val outSelVec = VecInit(PriorityEncoderOH(outMatchVec))
//     val outSelRespVec = RegEnable(outSelVec,
//                                   VecInit(Seq.fill(outSelVec.length)(false.B)),
//                                   io.in.req.fire && state === s_idle)
// 
//     val reqInvalidAddr = io.in.req.valid && !outSelVec.asUInt.orR
// 
//     switch (state) {
//         is (s_idle) {
//             when (io.in.req.fire && ~io.flush) { state := s_resp}
//             when (reqInvalidAddr && ~io.flush) { state := s_error}
//         }
//         is (s_resp)  {when (io.in.resp.fire) {state := s_idle}}
//         is (s_error) {when (io.in.resp.fire) {state := s_idle}}
//     }
// 
//     // in.req, only receive access in idle
//     io.in.req.ready  := Mux1H(outSelVec, io.out.map(_.req.ready)) && state === s_idle || reqInvalidAddr
// 
//     // in.resp
//     io.in.resp.valid := Mux1H(outSelRespVec, io.out.map(_.resp.valid)) && state === s_resp || state === s_error
//     io.in.resp.bits  := Mux1H(outSelRespVec, io.out.map(_.resp.bits))
// 
//     // out.req
//     for (i <- 0 until io.out.length) {
//       io.out(i).req.valid := outSelVec(i) && io.in.req.valid && state === s_idle
//       io.out(i).req.bits  := io.in.req.bits
//     }
// 
//     // out.resp
//     for (i <- 0 until io.out.length) {
//         io.out(i).resp.ready := outSelRespVec(i) && io.in.resp.ready && state === s_resp
//     }
// }

class SimpleBusCrossBar1toN(addressSpace: List[(Long, Long)]) extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(new SimpleBus)
        val out = Vec(addressSpace.length, new SimpleBus)
        val flush = Input(Bool())
    })

    val s_idle :: s_resp :: s_error :: Nil = Enum(3)
    val state = RegInit(s_idle)

    // select the output channel according to the address
    val addr = io.in.req.bits.addr
    val outMatchVec = VecInit(addressSpace.map(
      range => (addr >= range._1.U && addr < (range._1 + range._2).U)))
    val outSelVec = VecInit(PriorityEncoderOH(outMatchVec))
    val outSelRespVec = RegEnable(outSelVec,
                                  VecInit(Seq.fill(outSelVec.length)(false.B)),
                                  io.in.req.fire && state === s_idle)

    val reqInvalidAddr = io.in.req.valid && !outSelVec.asUInt.orR

    switch (state) {
        is (s_idle) {
          when (io.in.resp.fire || io.flush) {
            state := s_idle
          } .elsewhen(io.in.req.fire) {
            state := s_resp
          } .elsewhen(reqInvalidAddr) {
            state := s_error
          }
        }
        is (s_resp) {
          // when(io.in.req.fire && io.in.resp.fire) {
          //   state := s_resp
          // } .elsewhen(io.in.resp.fire) {
          //   state := s_idle
          // } .otherwise {
          //   state := s_resp
          // }
          state := Mux(io.in.resp.fire, s_idle, s_resp)
        }
        is (s_error) {state := s_idle}
    }

    // in.req, only receive access in idle
    // io.in.req.ready  := MuxLookup(state, false.B)(List(
    //   s_idle -> Mux1H(outSelVec, io.out.map(_.req.ready)),
    //   s_resp -> Mux1H(outSelRespVec, io.out.map(_.req.ready)),
    // ))
    io.in.req.ready := Mux1H(outSelVec, io.out.map(_.req.ready)) && state === s_idle

    // in.resp
    io.in.resp.valid := MuxLookup(state, false.B)(List(
      s_idle -> Mux1H(outSelVec, io.out.map(_.resp.valid)),
      s_resp -> Mux1H(outSelRespVec, io.out.map(_.resp.valid)),
    ))
    io.in.resp.bits  := MuxLookup(state, 0.U.asTypeOf(io.in.resp.bits))(List(
      s_idle -> Mux1H(outSelVec, io.out.map(_.resp.bits)),
      s_resp -> Mux1H(outSelRespVec, io.out.map(_.resp.bits)),
    ))

    // out.req
    for (i <- 0 until io.out.length) {
      io.out(i).req.valid := outSelVec(i) && io.in.req.valid && state === s_idle
      io.out(i).req.bits  := io.in.req.bits
    }

    // out.resp
    for (i <- 0 until io.out.length) {
      io.out(i).resp.ready := io.in.resp.ready && MuxLookup(state, false.B)(List(
        s_idle -> outSelVec(i),
        s_resp -> outSelRespVec(i),
      ))
    }
}

/**
  * USAGE:
  * dcache & icache -> mem
  */
class SimpleBusCrossBarNto1 extends Module {

}