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
import _root_.dataclass.data

class Icache_SimpleBus extends Module {
    val from_ifu = IO(Flipped(new SimpleBus))
    val to_sram  = IO(new AXI4) // to Arbiter

    val replace_set = RegInit(0.U)
    val EntId       = from_ifu.req.bits.addr(ent_MSB, ent_LSB)
    val CacheLineId = from_ifu.req.bits.addr(idx_MSB, idx_LSB)
    val tag         = from_ifu.req.bits.addr(tag_MSB, tag_LSB)

    // as ramdom number when evict cache line
    val random_num = RegInit(0.U(numSetsWidth.W))
    random_num := random_num+1.U

    // val dataArray  = SyncReadMem(numSets, Vec(numCacheLine, Vec(numEnts, UInt(DATA_WIDTH.W))))
    val dataArray  = SyncReadMem(numSets*numCacheLine*numEnts, UInt(DATA_WIDTH.W))
    val tagArray   = RegInit(VecInit(Seq.fill(numSets)(VecInit(Seq.fill(numCacheLine)(0.U(tagWidth.W))))))
    val validArray = RegInit(VecInit(Seq.fill(numSets)(VecInit(Seq.fill(numCacheLine)(false.B)))))

    val hitArray = for (i <- 0 until numSets) yield {
        (tag === tagArray(i)(CacheLineId)) && validArray(i)(CacheLineId)
    }
    val hit = hitArray.reduce(_ | _) // use or on all elem in hitArray

    val tagSeq = for (i <- 0 until numSets) yield {
        tagArray(i)(CacheLineId) -> i.U
    }
    val SetId = MuxLookup(tag, 0.U)(tagSeq)

    val hitCacheAddr = Cat(SetId, CacheLineId, EntId) // 

    val off   = RegInit(0.U((offWidth-DATA_BYTE_WIDTH).W))

    val s_idle :: s_read_valid :: s_rq :: s_reading :: s_end :: Nil = Enum(5)
    val state_cache = RegInit(s_idle)
    switch (state_cache) {
        is (s_idle) {
            when (from_ifu.req.fire) {
                state_cache := Mux(hit, s_read_valid, s_rq)
            } .otherwise {
                state_cache := s_idle
            }
        }
        is (s_read_valid) {
            state_cache := s_idle
        }
        // read request
        is (s_rq) {
            state_cache := Mux(to_sram.ar.fire, s_reading, s_rq)
            off         := 0.U
            replace_set := random_num
        }
        // wait for data transfer
        is (s_reading) {
            state_cache := Mux(to_sram.r.bits.last, s_end, s_reading)
            off         := Mux(to_sram.r.fire, off+1.U, off)
        }
        // for the last data in one cache line write into it, we can not read and write one entry 
        // simultaneously, and in s_read_valid, we read it. in end state, we write the last entry into cache line
        is (s_end) {
            state_cache := s_read_valid
        }
    }

    // val cachedata = dataArray(SetId)(CacheLineId)(EntId)
    val cachedata = dataArray(hitCacheAddr)
    // can move the following codes to "is (s_reading)" state 
    val replaceCacheAddr = Cat(replace_set, CacheLineId, off)
    when ((state_cache === s_reading) && to_sram.r.fire) {
        dataArray(replaceCacheAddr) := to_sram.r.bits.data
        when(to_sram.r.bits.last) {
            validArray(replace_set)(CacheLineId) := true.B
            tagArray(replace_set)(CacheLineId)   := tag
        }
    }

    from_ifu.req.ready       := MuxLookup(state_cache, false.B)(List(s_idle -> true.B))
    from_ifu.resp.valid      := MuxLookup(state_cache, false.B)(List(s_read_valid -> true.B))
    from_ifu.resp.bits.rdata := Mux(hit, cachedata, NOP.U)
    from_ifu.resp.bits.wresp := DontCare

    to_sram.ar.valid      := MuxLookup(state_cache, false.B)(List(s_rq -> true.B))
    to_sram.ar.bits.addr  := MuxLookup(state_cache, 0.U)(List(s_rq -> ((from_ifu.req.bits.addr>>offWidth.U)<<offWidth.U)))
    to_sram.ar.bits.size  := MuxLookup(state_cache, 0.U)(List(s_rq -> DATA_WIDTH.U))
    to_sram.ar.bits.len   := MuxLookup(state_cache, 0.U)(List(s_rq -> (numEnts-1).U)) //transfer anlen+1 items
    to_sram.ar.bits.burst := INCR.U
    to_sram.r.ready       := MuxLookup(state_cache, false.B)(List(s_reading ->  true.B))
    to_sram.aw.valid      := false.B
    to_sram.aw.bits.addr  := 0.U
    to_sram.aw.bits.size  := 0.U
    to_sram.aw.bits.len   := 0.U
    to_sram.aw.bits.burst := 0.U
    to_sram.w.valid       := false.B
    to_sram.w.bits.data   := 0.U
    to_sram.w.bits.strb   := 0.U
    to_sram.w.bits.last   := false.B
    to_sram.b.ready       := false.B
}

/**
  * two stage pipelined icache
  *
  */
class Icache_pipeline extends Module {
    val from_ifu = IO(Flipped(new SimpleBus))
    val to_sram  = IO(new AXI4) // to Arbiter
    val redirect = IO(Input(Bool()))
    val instPC   = IO(Output(UInt(ADDR_WIDTH.W)))

    val replace_set = RegInit(0.U)
    val EntId       = from_ifu.req.bits.addr(ent_MSB, ent_LSB)
    val CacheLineId = from_ifu.req.bits.addr(idx_MSB, idx_LSB)
    val tag         = from_ifu.req.bits.addr(tag_MSB, tag_LSB)

    // as ramdom number when evict cache line
    val random_num = RegInit(0.U(numSetsWidth.W))
    random_num := random_num+1.U

    /* 1 cycle delay read */
    val dataArray  = SyncReadMem(numSets*numCacheLine*numEnts, UInt(DATA_WIDTH.W))
    val tagArray   = RegInit(VecInit(Seq.fill(numSets)(VecInit(Seq.fill(numCacheLine)(0.U(tagWidth.W))))))
    val validArray = RegInit(VecInit(Seq.fill(numSets)(VecInit(Seq.fill(numCacheLine)(false.B)))))

    val hitArray = for (i <- 0 until numSets) yield {
        (tag === tagArray(i)(CacheLineId)) && validArray(i)(CacheLineId)
    }
    val hit = hitArray.reduce(_ | _) // use or on all elem in hitArray

    val tagSeq = for (i <- 0 until numSets) yield {
        tagArray(i)(CacheLineId) -> i.U
    }
    val SetId = MuxLookup(tag, 0.U)(tagSeq)

    val hitCacheAddr = Cat(SetId, CacheLineId, EntId) // 

    val off   = RegInit(0.U((offWidth-DATA_BYTE_WIDTH).W))

    val s_idle :: s_rq :: s_reading :: s_end :: Nil = Enum(4)
    val state_cache = RegInit(s_idle)
    switch (state_cache) {
        is (s_idle) {
            // state_cache := Mux(~hit && from_ifu.resp.ready, s_rq, s_idle)
            state_cache := Mux(~hit, s_rq, s_idle)
        }
        // read request
        is (s_rq) {
            state_cache := Mux(to_sram.ar.fire, s_reading, s_rq)
            off         := 0.U
            replace_set := random_num
        }
        // wait for data transfer
        is (s_reading) {
            state_cache := Mux(to_sram.r.bits.last, s_end, s_reading)
            off         := Mux(to_sram.r.fire, off+1.U, off)
        }
        // for the last data in one cache line write into it, we can not read and write one entry 
        // simultaneously, and in s_read_valid, we read it. in end state, we write the last entry into cache line
        is (s_end) {
            state_cache := s_idle
        }
    }

    val cachedata = dataArray(hitCacheAddr)

    // can move the following codes to "is (s_reading)" state 
    val replaceCacheAddr = Cat(replace_set, CacheLineId, off)
    when ((state_cache === s_reading) && to_sram.r.fire) {
        dataArray(replaceCacheAddr) := to_sram.r.bits.data
        when(to_sram.r.bits.last) {
            validArray(replace_set)(CacheLineId) := true.B // when read the last make it valid
            tagArray(replace_set)(CacheLineId)   := tag
        }
    }

    val dataValid = RegInit(false.B)
    dataValid := hit && from_ifu.resp.ready

    val instPCReg = RegInit(0.U(ADDR_WIDTH.W))
    
    when(from_ifu.resp.ready) {
        instPCReg := from_ifu.req.bits.addr
    }

    instPC := instPCReg

    // when(from_ifu.resp.fire) {dataValid := false.B}
    // when(hit && from_ifu.resp.ready) {dataValid := true.B}

    // val instReg = RegInit(0xdeadbeefL.U(INST_WIDTH.W))
    // when(~from_ifu.resp.ready && (instReg === 0xdeadbeefL.U )) {instReg := cachedata}
    // .elsewhen(redirect) {instReg := 0.U}
    // .otherwise {instReg := 0xdeadbeefL.U}


    val instReg      = RegInit(0.U(INST_WIDTH.W))
    val instRegValid = RegInit(false.B)

    // when(~from_ifu.resp.ready && ~instRegValid && dataValid) {
    //     instReg := cachedata
    //     instRegValid := true.B
    // } .elsewhen(redirect) {
    //     instReg := 0.U
    //     dataValid := false.B // flushed
    //     // instRegValid := true.B
    // } .elsewhen(instRegValid  && from_ifu.resp.fire) {
    //     instRegValid := false.B
    // }
    when (redirect) {
        instReg := 0.U
        dataValid := false.B // flushed
        // instRegValid := true.B
    } .elsewhen (~from_ifu.resp.ready && ~instRegValid && dataValid) {
        instReg := cachedata
        instRegValid := true.B
    } .elsewhen(instRegValid  && from_ifu.resp.fire) {
        instRegValid := false.B
    }
 

    // when the data has been read from mem, just transfer it to ifu, do not need to wait all data in a cache line been read, critical word first
    from_ifu.req.ready       := hit && (state_cache === s_idle || state_cache === s_end)
    from_ifu.resp.valid      := (dataValid && state_cache === s_idle) || (instReg =/= 0.U && instRegValid) //flushed, stored
    // from_ifu.resp.valid      := (dataValid) || (instReg =/= 0.U && instRegValid) //flushed, stored, easy error!
    from_ifu.resp.bits.rdata := Mux(instRegValid, instReg, cachedata)
    from_ifu.resp.bits.wresp := DontCare

    to_sram.ar.valid      := MuxLookup(state_cache, false.B)(List(s_rq -> true.B))
    to_sram.ar.bits.addr  := MuxLookup(state_cache, 0.U)(List(s_rq -> ((from_ifu.req.bits.addr>>offWidth.U)<<offWidth.U)))
    to_sram.ar.bits.size  := MuxLookup(state_cache, 0.U)(List(s_rq -> DATA_WIDTH.U))
    to_sram.ar.bits.len   := MuxLookup(state_cache, 0.U)(List(s_rq -> (numEnts-1).U)) //transfer anlen+1 items
    to_sram.ar.bits.burst := INCR.U
    to_sram.r.ready       := MuxLookup(state_cache, false.B)(List(s_reading ->  true.B))
    to_sram.aw.valid      := false.B
    to_sram.aw.bits.addr  := 0.U
    to_sram.aw.bits.size  := 0.U
    to_sram.aw.bits.len   := 0.U
    to_sram.aw.bits.burst := 0.U
    to_sram.w.valid       := false.B
    to_sram.w.bits.data   := 0.U
    to_sram.w.bits.strb   := 0.U
    to_sram.w.bits.last   := false.B
    to_sram.b.ready       := false.B
}
