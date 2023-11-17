package rv32e.cache

import chisel3._
import chisel3.util._
import rv32e.config.Configs._
import rv32e.config.Cache_Configs._
import rv32e.config.Axi_Configs._
import rv32e.define.Dec_Info._
import rv32e.define.Inst._
import rv32e.define.CSR_Info._
import rv32e.utils.DiffCsr
import rv32e.bus._

class D_Cache extends Module {
    val from_LSU = IO(Flipped(Decoupled(new LSU2Cache_bus)))
    val to_LSU   = IO(Decoupled(new Cache2LSU_bus))
    val to_sram  = IO(new AXIIO_master)
    
    val replace_set = RegInit(0.U)
    val ByteId      = from_LSU.bits.addr(byte_MSB, byte_LSB)
    val EntId       = from_LSU.bits.addr(ent_MSB, ent_LSB)
    val CacheLineId = from_LSU.bits.addr(idx_MSB, idx_LSB)
    val tag         = from_LSU.bits.addr(tag_MSB, tag_LSB)

    // as ramdom number when evict cache line
    val random_num = RegInit(0.U(numSetsWidth.W))
    random_num     := random_num+1.U

    // maybe we need not align to 4 in dcahe, we coult just align to 1
    val dataArray  = SyncReadMem(numSets, Vec(numCacheLine, Vec(numEnts, UInt(DATA_WIDTH.W))))
    // val dataArray  = SyncReadMem(numSets, SyncReadMem(numCacheLine, Vec(numEnts, UInt(DATA_WIDTH.W))))
    val tagArray   = RegInit(VecInit(Seq.fill(numSets)(VecInit(Seq.fill(numCacheLine)(0.U(tagWidth.W))))))
    val validArray = RegInit(VecInit(Seq.fill(numSets)(VecInit(Seq.fill(numCacheLine)(false.B)))))
    val dirtyArray = RegInit(VecInit(Seq.fill(numSets)(VecInit(Seq.fill(numCacheLine)(false.B)))))

    val hitArray = for (i <- 0 until numSets) yield {
        (tag === tagArray(i)(CacheLineId)) && validArray(i)(CacheLineId)
    }
    val hit = hitArray.reduce(_ | _) // use or on all elem in hitArray

    val tagSeq = for (i <- 0 until numSets) yield {
        tagArray(i)(CacheLineId) -> i.U
    }
    val SetId = MuxLookup(tag, 0.U, tagSeq)

    val dirty = dirtyArray(replace_set)(CacheLineId)

    val off   = RegInit(0.U((offWidth-DATA_BYTE_WIDTH).W))

    val cacheAddr = Cat(SetId, CacheLineId, EntId) // 

    val s_idle :: s_rresp :: s_wresp :: s_replace :: s_wrq :: s_writing :: s_wend :: s_rrq :: s_reading :: s_rend :: Nil = Enum(10)
    val state_dcache = RegInit(s_idle)
    switch (state_dcache) {
        is (s_idle) {
            when (from_LSU.fire) {
                when (hit) {
                    state_dcache := Mux(from_LSU.bits.is_write, s_wresp, s_rresp)
                } .otherwise {
                    replace_set  := random_num
                    state_dcache := s_replace
                }
            } .otherwise {
                state_dcache := s_idle
            }
        }
        is (s_rresp) {
            state_dcache := s_idle
        }
        is (s_wresp) {
            state_dcache := s_idle
        }
        is (s_replace) {
            state_dcache := Mux(dirty, s_wrq, s_rrq)
        }
        is (s_wrq) {
            state_dcache := Mux(to_sram.aw.fire, s_writing, s_wrq)
            off          := 0.U
        }
        is (s_writing) {
            state_dcache := Mux(to_sram.w.bits.last, s_wend, s_writing)
            off          := Mux(to_sram.w.fire, off+1.U, off)
        }
        is (s_wend) {
            state_dcache := s_rrq
        }
        // read request
        is (s_rrq) {
            state_dcache := Mux(to_sram.ar.fire, s_reading, s_rrq)
            off         := 0.U
        }
        // wait for data transfer
        is (s_reading) {
            state_dcache := Mux(to_sram.r.bits.last, s_rend, s_reading)
            off          := Mux(to_sram.r.fire, off+1.U, off)
        }
        // for the last data in one cache line write into it, we can not read and write one entry 
        // simultaneously, and in s_read_valid, we read it. in end state, we write the last entry into cache line
        is (s_rend) {
            state_dcache := Mux(from_LSU.bits.is_write, s_wresp, s_rresp)
        }
    }

    // read from cache, delay one cycle
    val cachedata = dataArray(SetId)(CacheLineId)(EntId)

    // read from mem
    when ((state_dcache === s_reading) && to_sram.r.fire) {
        dataArray(replace_set)(CacheLineId)(off) := to_sram.r.bits.data
        when(to_sram.r.bits.last) {
            validArray(replace_set)(CacheLineId) := true.B
            tagArray(replace_set)(CacheLineId)   := tag
        }
    }

    // wrting to cache, 3 cycles: 1 issue address, 2 read cache, 3 write cache.
    // we can also use the minimal unit of cache is 1 byte, not 4 fytes, than we can only use 2 cycles:
    //  1 issue address, 2 write cache
    val wdata   = from_LSU.bits.wdata
    val wmask   = from_LSU.bits.wmask
    val outdata = dataArray(SetId)(CacheLineId)(EntId)
    val indata  = (((wdata << (BYTE_WIDTH.U * ByteId)) & wmask) | (outdata & ~wmask))
    when ((state_dcache === s_wresp)) {
        dataArray(SetId)(CacheLineId)(EntId) := indata
        dirtyArray(SetId)(CacheLineId)       := true.B
    }

    // wrting to mem
    val toMemData = dataArray(replace_set)(CacheLineId)(off)

    // signals
    from_LSU.ready := MuxLookup(state_dcache, false.B, List(s_idle -> true.B))

    to_sram.ar.valid      := MuxLookup(state_dcache, false.B, List(s_rrq -> true.B))
    to_sram.ar.bits.addr  := MuxLookup(state_dcache, 0.U, List(s_rrq -> ((from_LSU.bits.addr>>offWidth.U)<<offWidth.U)))
    to_sram.ar.bits.size  := MuxLookup(state_dcache, 0.U, List(s_rrq -> DATA_WIDTH.U))
    to_sram.ar.bits.len   := MuxLookup(state_dcache, 0.U, List(s_rrq -> (numEnts-1).U)) //transfer anlen+1 items
    to_sram.ar.bits.burst := INCR.U
    to_sram.r.ready       := MuxLookup(state_dcache, false.B, List(s_reading ->  true.B))
    to_sram.aw.valid      := MuxLookup(state_dcache, false.B, List(s_wrq -> true.B))
    to_sram.aw.bits.addr  := MuxLookup(state_dcache, 0.U, List(s_wrq -> ((from_LSU.bits.addr>>offWidth.U)<<offWidth.U)))
    to_sram.aw.bits.size  := MuxLookup(state_dcache, 0.U, List(s_wrq -> DATA_WIDTH.U))
    to_sram.aw.bits.len   := MuxLookup(state_dcache, 0.U, List(s_wrq -> (numEnts-1).U)) //transfer anlen+1 items
    to_sram.aw.bits.burst := INCR.U
    to_sram.w.valid       := MuxLookup(state_dcache, false.B, List(s_writing ->  true.B))
    to_sram.w.bits.data   := toMemData
    to_sram.w.bits.strb   := "b1111".U
    to_sram.w.bits.last   := off === numEnts.U
    to_sram.b.ready       := MuxLookup(state_dcache, false.B, List(s_wend -> true.B))

    to_LSU.bits.data  := Mux(hit, cachedata, 0.U)
    to_LSU.bits.bresp := MuxLookup(state_dcache, false.B, List(s_wresp -> true.B))
    to_LSU.valid      := MuxLookup(state_dcache, false.B, List(s_rresp -> true.B, s_wresp -> true.B))
}

