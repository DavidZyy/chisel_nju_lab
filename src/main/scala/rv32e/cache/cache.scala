package rv32e.cache

import chisel3._
import chisel3.util._
import rv32e.config.Configs._
import rv32e.config.Cache_Configs._
import rv32e.define.Dec_Info._
import rv32e.define.Inst._
import rv32e.define.CSR_Info._
import rv32e.utils.DiffCsr
import rv32e.bus._

class I_Cache extends Module {
    val from_IFU = IO(Flipped(Decoupled(new IFU2Cache_bus)))
    val to_IFU   = IO(Decoupled(new Cache2IFU_bus))
    // val to_Arb   = IO(new AXILiteIO_master) // to Arbiter
    val to_sram  = IO(new AXILiteIO_master) // to Arbiter

    val EntId       = from_IFU.bits.addr(off_MSB, off_LSB)
    val CacheLineId = from_IFU.bits.addr(idx_MSB, idx_LSB)
    val tagId       = from_IFU.bits.addr(tag_MSB, tag_LSB)

    val s_idle :: s_read_valid :: s_read_sram :: s_wait :: s_end :: Nil = Enum(5)
    val state = RegInit(s_idle)

    // as ramdom number when evict cache line
    val SetId = RegInit(0.U(numSetsWidth.W))
    SetId := SetId+1.U

    val dataArray  = SyncReadMem(numSets, Vec(numCacheLine, Vec(numEnts, UInt(DATA_WIDTH.W))))
    val tagArray   = RegInit(VecInit(Seq.fill(numSets)(VecInit(Seq.fill(numCacheLine)(0.U(tagWidth.W))))))
    val validArray = RegInit(VecInit(Seq.fill(numSets)(VecInit(Seq.fill(numCacheLine)(0.U(Bool()))))))
    val dirtyArray = RegInit(VecInit(Seq.fill(numSets)(VecInit(Seq.fill(numCacheLine)(0.U(Bool()))))))

    val hit_set = MuxLookup(tagId, 0.U, List(
        tagArray(0)(numCacheLine) -> 0.U,        
        tagArray(1)(numCacheLine) -> 1.U,
    ))

    val _hit = (Mux(tagId === tagArray(0)(numCacheLine), true.B, false.B) && validArray(0)(numCacheLine)) |
               (Mux(tagId === tagArray(1)(numCacheLine), true.B, false.B) && validArray(1)(numCacheLine))

    val hit  = from_IFU.fire && _hit
    val miss = from_IFU.fire && ~hit

    switch (state) {
        is (s_idle) {
            when (hit) {
                state := s_read_valid
            }.elsewhen(miss) {
                state := s_read_sram
            }.otherwise {
                state := s_idle
            }
        }
        is (s_read_valid) {
            state := s_idle
        }
        // issue request
        is (s_read_sram) {
            state := s_wait 
        }
        // wait for data transfer
        is (s_wait) {
            // state := Mux(to_sram.r.bits.last, s_read_valid, s_wait)

            // state := Mux(to_sram.r.bits.last, s_end, s_wait)
        }
        // end, maybe can be ignore, wait ot read valid directly
        is (s_end) {
            state := s_read_valid
        }
    }

    from_IFU.ready := MuxLookup(state, false.B, List(s_idle -> true.B))

    // to_IFU.bits.data := DontCare
    // to_IFU.bits.data := Mux(NOP, rr)

}
