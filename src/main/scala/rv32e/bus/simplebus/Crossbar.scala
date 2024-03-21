package rv32e.bus.simplebus

import os.list

import chisel3._
import chisel3.util._

import rv32e.bus._
import rv32e.bus.simplebus.SimpleBusReqBundle
 
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

    val cmdReg = RegInit(0.U(4.W))

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

    // out.resp, outSelVec is one hot, so if outSelVec(i) is true, it means the device i is
    // ready to get response.
    for (i <- 0 until io.out.length) {
      io.out(i).resp.ready := io.in.resp.ready && MuxLookup(state, false.B)(List(
        s_idle -> outSelVec(i),
        s_resp -> outSelRespVec(i),
      ))
    }
}

/**
  * USAGE:
  * dcache & icache & mmio -> Xbar -> Soc
  */
class SimpleBusCrossBarNto1(n: Int) extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Vec(n, new SimpleBus))
    val out = new SimpleBus
    val flush = Input(Bool())
  })

  val s_idle :: s_resp :: Nil = Enum(2)
  val state = RegInit(s_idle)

  // i think it's not necessary use the lib arbiter
  val inputArb = Module(new Arbiter(chiselTypeOf(io.in(0).req.bits), n))

  val inflightSrc = RegInit(0.U(log2Up(n).W))

  //////// bind req channel /////////////
  for(i <- 0 until n) {
    inputArb.io.in(i) <> io.in(i).req
  }

  val thisReq = inputArb.io.out

  io.out.req.valid := thisReq.valid && ((state === s_idle) || inputArb.io.chosen === inflightSrc) // the later condition for write burst, it issue aw valid and then w valid
  io.out.req.bits  := thisReq.bits
  thisReq.ready    := io.out.req.ready && ((state === s_idle) || inputArb.io.chosen === inflightSrc)
  //////// bind req channel /////////////


  //////// bind resp channel /////////////
  io.out.resp.ready := io.in(inflightSrc).resp.ready

  for (i <- 0 until n) {
    io.in(i).resp.bits := io.out.resp.bits
    io.in(i).resp.valid := Mux(i.asUInt === inflightSrc, 
                                io.out.resp.valid,
                                false.B)
  }
  //////// bind resp channel /////////////

  switch (state) {
    is (s_idle) {
      when(thisReq.fire && !io.flush) {
        inflightSrc := inputArb.io.chosen
        state := s_resp
      }
    }
    is (s_resp) {
      when(io.out.resp.fire || io.flush) {
        state := s_idle
      }
    }
  }
}
