package rv32e.bus

import chisel3._
import chisel3.util._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import rv32e.config.Configs._
import rv32e.define.Dec_Info._
import rv32e.define.Inst._
import rv32e.utils.LFSR


// is not idle, the requesting master will be blocked in request state.
class Arbiter extends Module {
    // master1 is ifu, master2 is lsu
    val from_master1 = IO(new AXILiteIO_slave)
    // val from_master2 = IO(new AXILiteIO_slave)
    val to_slave     = IO(new AXILiteIO_master)

    // val states = Enum(13)
    val states = Enum(13)
    val (
        s_idle :: 
        // receive read request
        s_rcv_rrq_m1 :: 
        s_read_rq_m1 :: 
        s_read_wait_m1 :: 
        s_read_end_m1 ::
        // receive read request
        s_rcv_rrq_m2 :: 
        s_read_rq_m2 :: 
        s_read_wait_m2 :: 
        s_read_end_m2 ::
        // receive write request
        s_rcv_wrq_m2 :: 
        s_write_rq_m2 :: 
        s_write_wait_m2 :: 
        s_write_end_m2 ::

        Nil
    ) = states

    val state = RegInit(s_idle)

    switch (state) {
        is (s_idle) {
            when (from_master1.ar.valid) {
                state := s_rcv_rrq_m1
            } 
            // .elsewhen (from_master2.ar.valid) {
            //     state := s_rcv_rrq_m2
            // } 
            // .elsewhen (from_master2.aw.valid && from_master2.w.valid) {
            //     state := s_rcv_wrq_m2
            // } 
            .otherwise {
                state := s_idle
            }
        }

        is (s_rcv_rrq_m1) {
            state := Mux(from_master1.ar.fire, s_read_rq_m1, s_rcv_rrq_m1)
        }
        is (s_read_rq_m1) {
            state := Mux(to_slave.ar.fire, s_read_wait_m1, s_read_rq_m1)
        }
        is (s_read_wait_m1) {
            state := Mux(to_slave.r.fire, s_read_end_m1, s_read_wait_m1)
        }
        is (s_read_end_m1) {
            state := Mux(from_master1.r.fire, s_idle, s_read_end_m1)
        }

        // is (s_rcv_rrq_m2) {
        //     state := Mux(from_master2.ar.fire, s_read_rq_m2, s_rcv_rrq_m2)
        // }
    }


    to_slave.ar.valid       := MuxLookup(state, false.B, List(
        s_read_rq_m1    ->  true.B,
        s_read_rq_m2    ->  true.B
    ))
    to_slave.ar.bits.addr   := MuxLookup(state, 0.U, List(
        s_read_rq_m1      ->  from_master1.ar.bits.addr,
        s_read_wait_m1    ->  from_master1.ar.bits.addr,

    ))
    to_slave.r.ready    := MuxLookup(state, false.B, List(
        s_read_wait_m1  ->  true.B,
        s_read_wait_m2  ->  true.B
    ))
    to_slave.aw.valid     :=    MuxLookup(state, false.B, List(s_write_rq_m2 -> true.B))
    to_slave.aw.bits.addr :=    0.U
    to_slave.w.valid      :=    MuxLookup(state, false.B, List(s_write_rq_m2 -> true.B))
    to_slave.w.bits.data  :=    0.U
    to_slave.w.bits.strb  :=    0.U
    to_slave.b.ready      :=    MuxLookup(state, false.B, List(s_write_wait_m2 -> true.B)) 


    from_master1.ar.ready    := MuxLookup(state, false.B, List(s_rcv_rrq_m1 -> true.B))
    from_master1.r.bits.data := to_slave.r.bits.data
    from_master1.r.valid     := MuxLookup(state, false.B, List(s_read_end_m1 -> true.B))
    from_master1.aw.ready    := false.B
    from_master1.w.ready     := false.B
    from_master1.b.valid     := false.B

}
