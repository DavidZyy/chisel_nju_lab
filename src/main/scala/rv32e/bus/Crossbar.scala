package rv32e.bus

import chisel3._
import chisel3.util._
 
/** 
  * USAGE:
  * lsu  -> dcache & mmio
  * mmio -> devs
  * @param addressSpace
  */
class SimpleBusCrossBar1toN(addressSpace: List[(Long, Long)]) extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(new SimpleBus)
        val out = Vec(addressSpace.length, new SimpleBus)
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
            when (io.in.req.fire) { state := s_resp}
            when (reqInvalidAddr) { state := s_error}
        }
        is (s_resp)  {when (io.in.resp.fire) {state := s_idle}}
        is (s_error) {when (io.in.resp.fire) {state := s_idle}}
    }

    // in.req
    io.in.req.ready  := Mux1H(outSelVec, io.out.map(_.req.ready)) || reqInvalidAddr

    // in.resp
    io.in.resp.valid := Mux1H(outSelRespVec, io.out.map(_.resp.valid)) || state === s_error
    io.in.resp.bits  := Mux1H(outSelRespVec, io.out.map(_.resp.bits))

    // out.req
    for (i <- 0 until io.out.length) {
      io.out(i).req.valid := outSelVec(i) && io.in.req.valid && state === s_idle
      io.out(i).req.bits  := io.in.req.bits
    }

    // out.resp
    for (i <- 0 until io.out.length) {
        io.out(i).resp.ready := outSelRespVec(i) && io.in.resp.ready && state === s_resp
    }
}

/**
  * USAGE:
  * dcache & icache -> mem
  */
class SimpleBusCrossBarNto1 extends Module {

}