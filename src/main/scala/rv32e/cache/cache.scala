package rv32e.cache

import chisel3._
import chisel3.util._
import rv32e.bus._
import rv32e.config.Configs._
import rv32e.utils._

trait HasCacheConst {
  val byteIdxWidth  = 2 // one entry contains 1<<$ bytes
  val entryIdxWidth = 2 // one cache line contains 1<<$ entries
  val setIdxWidth   = 4 // one cache way contains 1<<$ sets
  val tagWidth      = ADDR_WIDTH - setIdxWidth - entryIdxWidth - byteIdxWidth

  val byteLSB = 0
  val byteMSB = byteLSB + byteIdxWidth - 1

  val entryLSB = byteMSB + 1  
  val entryMSB = entryLSB + entryIdxWidth - 1

  val setLSB = entryMSB + 1
  val setMSB = setLSB + setIdxWidth - 1

  val tagLSB = setMSB + 1 // tag starts right after the set index
  val tagMSB = tagLSB + tagWidth - 1

  val wayIdxWidth = 1
  val wayCnt      = 1<<wayIdxWidth // have this number ways
  val setCnt      = 1<<setIdxWidth
  val entryCnt    = 1<<entryIdxWidth

  val addrWidth   = wayIdxWidth + setIdxWidth + entryIdxWidth // entry's addr in sram
}

class Stage1IO extends Bundle with HasCacheConst {
  val addr = Output(UInt(ADDR_WIDTH.W))
}

class Stage2IO extends Bundle with HasCacheConst {
  val addr  = Output(UInt(ADDR_WIDTH.W))
  val rdata = Output(UInt(DATA_WIDTH.W))
}

/**
  * simplebus + two staged pipeline, can used by icache and dcache.
  */
class CacheStage1(val dataWidth: Int) extends Module with HasCacheConst {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new SimpleBusReqBundle)) // from cache.in.req
    val mem = new SimpleBus // to and from mem
    val out = Decoupled(new Stage1IO) // to Cachestage2
    val dataReadBus  = Decoupled(new SRAMBundleReadReq(addrWidth)) // to sram
    val dataWriteBus = new SRAMWriteBus(addrWidth, dataWidth) // to sram
  })

  val replaceWayReg = RegInit(0.U)
  val entryIdx = io.in.bits.addr(entryMSB, entryLSB)
  val setIdx   = io.in.bits.addr(setMSB, setLSB)
  val tag      = io.in.bits.addr(tagMSB, tagLSB)

  val randomNum = RegInit(0.U(wayIdxWidth.W))
  randomNum := randomNum+1.U

  val tagArray   = RegInit(VecInit(Seq.fill(wayCnt)(VecInit(Seq.fill(setCnt)(0.U(tagWidth.W))))))
  val validArray = RegInit(VecInit(Seq.fill(wayCnt)(VecInit(Seq.fill(setCnt)(false.B)))))

  val hitArray = for (i <- 0 until wayCnt) yield {
      (tag === tagArray(i)(setIdx)) && validArray(i)(setIdx)
  }

  val hit = hitArray.reduce(_ | _) // use or on all elem in hitArray

  val tagSeq = for (i <- 0 until wayCnt) yield {
    tagArray(i)(setIdx) -> i.U
  }

  val wayIdx = MuxLookup(tag, 0.U)(tagSeq)

  val hitCacheAddr = Cat(wayIdx, setIdx, entryIdx)

  val entryOff = RegInit(0.U(entryIdxWidth.W))
  val last     = entryOff === ((1 << entryIdxWidth)-1).U

  val s_idle :: s_read_valid :: s_rq :: s_reading :: s_end :: Nil = Enum(5)
  val stateCache = RegInit(s_idle)
  switch (stateCache) {
    is (s_idle) {
      stateCache := Mux(~hit, s_rq, s_idle)
    } 
    is (s_rq) {
      stateCache    := Mux(io.mem.req.fire && io.mem.isRead, s_reading, s_rq)
      entryOff      := 0.U
      replaceWayReg := randomNum
    }
    is (s_reading) {
      stateCache := Mux(last, s_end, s_reading)
      entryOff   := Mux(io.mem.resp.fire, entryOff+1.U, entryOff)
    }
    is (s_end) {
      stateCache := s_idle
    }
  }

  when(last) {
    validArray(replaceWayReg)(setIdx) := true.B // when read the last make it valid
    tagArray(replaceWayReg)(setIdx)   := tag
  }

  val writeCacheAddr = Cat(replaceWayReg, setIdx, entryOff)

  io.out.valid     := hit
  io.out.bits.addr := io.in.bits.addr

  io.dataReadBus.valid      := io.in.valid 
  io.dataReadBus.bits.raddr := hitCacheAddr 

  // cache missing
  io.mem.req.valid     := MuxLookup(stateCache, false.B)(List(s_rq -> true.B))
  io.mem.req.bits.cmd  := SimpleBusCmd.read
  io.mem.req.bits.addr := (io.in.bits.addr>>byteIdxWidth.U)<<byteIdxWidth.U
  io.mem.req.bits.len  := (entryCnt-1).U

  io.dataWriteBus.req.valid      := (stateCache === s_reading) && io.mem.req.fire
  io.dataWriteBus.req.bits.waddr := writeCacheAddr
  io.dataWriteBus.req.bits.wdata := io.mem.resp.bits.rdata
}

class CacheStage2(val dataWidth: Int) extends Module with HasCacheConst {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new Stage1IO)) // from stage 1
    val dataReadBus = new SRAMBundleReadResp(dataWidth) // from sram
    val out = Decoupled(new Stage2IO)// to cache.in.resp
  })

  io.in.ready       := io.out.ready
  
  io.out.valid      := io.in.valid
  io.out.bits.rdata := io.dataReadBus.rdata
  io.out.bits.rdata := Mux(io.in.valid, io.dataReadBus.rdata, 0.U)
  io.out.bits.addr  := io.in.bits.addr
}

class Cache(val dataWidth: Int) extends Module with HasCacheConst {
  val io = IO(new Bundle {
    val in    = Flipped(new SimpleBus) // from ifu, lsu
    val mem   = new SimpleBus // to mem
    val flush = Input(Bool())
    val stage2Addr = Output(UInt(ADDR_WIDTH.W))
  })

  val dataArray = Module(new SRAMTemplate(addrWidth, dataWidth))
  val s1 = Module(new CacheStage1(dataWidth))
  val s2 = Module(new CacheStage2(dataWidth))

  // s1
  s1.io.in  <> io.in.req
  s1.io.mem <> io.mem
  s1.io.dataReadBus  <> dataArray.io.r.req
  s1.io.dataWriteBus <> dataArray.io.w
  PipelineConnect(s1.io.out, s2.io.in, s2.io.out.fire, io.flush)

  // s2
  s2.io.dataReadBus <> dataArray.io.r.resp

  // cache
  io.stage2Addr := s2.io.out.bits.addr
  io.in.resp.bits.rdata := s2.io.out.bits.rdata 
}
