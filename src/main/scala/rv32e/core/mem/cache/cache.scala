/**
  * dataWidth in cache module is so ugly, this parameter is finally tansfer to DataBundle, 
  * but I have no method to remove it, for in a 64 bit cpu, the data in icache is instructions, is 32 bits,
  * and the data in dcache is data, is 64 bits.
  */
package rv32e.core.mem.cache

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

import rv32e.bus._
import rv32e.bus.simplebus._

import rv32e.core.config._
import rv32e.core.define.Dec_Info._
import rv32e.core.define.Mem._
import rv32e.core.backend.fu._

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

  def CacheDataArrayReadBus(dataWidth: Int)  = new SRAMReadBus(new DataBudle(dataWidth), addrWidth)
  def CacheDataArrayWriteBus(dataWidth: Int) = new SRAMWriteBus(new DataBudle(dataWidth), addrWidth)
}

class DataBudle(dataWidth: Int) extends Bundle {
  val data = UInt(dataWidth.W)
}

class Stage1IO extends Bundle with HasCacheConst {
  val addr     = Output(UInt(ADDR_WIDTH.W))
  val is_write = Output(Bool())
  val waddr    = Output(UInt(addrWidth.W))
  val wmask    = Output(UInt(wmaskWidth.W))
  val wdata    = Output(UInt(DATA_WIDTH.W))
}

class Stage2IO extends Bundle with HasCacheConst {
  val addr  = Output(UInt(ADDR_WIDTH.W))
  val resp  = Decoupled(new SimpleBusRespBundle)
}

class CacheStage1(val dataWidth: Int, val cacheName: String) extends Module with HasCacheConst {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new SimpleBusReqBundle)) // from cache.in.req
    val mem = new SimpleBus // to and from mem
    val out = Decoupled(new Stage1IO) // to Cachestage2
    val dataReadBus  = Flipped(CacheDataArrayReadBus(dataWidth)) // to sram
    val dataWriteBus = Flipped(CacheDataArrayWriteBus(dataWidth)) // to sram, data refill
    val flush = Input(Bool())
  })

  val replaceWayReg = RegInit(0.U)
  val byteIdx  = io.in.bits.addr(byteMSB, byteLSB)
  val entryIdx = io.in.bits.addr(entryMSB, entryLSB)
  val setIdx   = io.in.bits.addr(setMSB, setLSB)
  val tag      = io.in.bits.addr(tagMSB, tagLSB)

  val randomNum = RegInit(0.U(wayIdxWidth.W))
  randomNum := randomNum+1.U

  val tagArray   = RegInit(VecInit(Seq.fill(wayCnt)(VecInit(Seq.fill(setCnt)(0.U(tagWidth.W))))))
  val validArray = RegInit(VecInit(Seq.fill(wayCnt)(VecInit(Seq.fill(setCnt)(false.B)))))
  val dirtyArray = RegInit(VecInit(Seq.fill(wayCnt)(VecInit(Seq.fill(setCnt)(false.B)))))

  val hitArray = for (i <- 0 until wayCnt) yield {
      (tag === tagArray(i)(setIdx)) && validArray(i)(setIdx)
  }

  val hit = hitArray.reduce(_ | _) // use or on all elem in hitArray

  // if have more than one tagArray have the same tag, there only 1 is valid, we can choose the valid 1 
  // or do not let more than one tagArray have the same tag.
  val tagSeq = for (i <- 0 until wayCnt) yield {
    tagArray(i)(setIdx) -> i.U
  }

  val wayIdx = MuxLookup(tag, 0.U)(tagSeq)

  val hitCacheAddr = Cat(wayIdx, setIdx, entryIdx)

  val entryOff = RegInit(0.U(entryIdxWidth.W))
  val last     = entryOff === ((1 << entryIdxWidth)-1).U

  val cacheSetDirty = dirtyArray(randomNum)(setIdx)

  // maybe should add a s_fillPipe state to move the s_fill in RAM to here cache.
  //  0         1        2        3            4            5
  val s_idle :: s_rrq :: s_wrq :: s_reading :: s_writing :: s_end :: Nil = Enum(6)
  val stateCache = RegInit(s_idle)
  switch (stateCache) {
    is (s_idle) {
      when(!hit && io.in.valid) {
        stateCache    := Mux(cacheSetDirty, s_wrq, s_rrq)
        replaceWayReg := randomNum
      } .otherwise {
        stateCache := s_idle
      }
    }
    is (s_rrq) {
      stateCache    := Mux(io.mem.req.fire, s_reading, s_rrq)
      entryOff      := 0.U
    }
    is (s_wrq) {
      stateCache    := Mux(io.mem.req.fire, s_writing, s_wrq)
      entryOff      := 0.U
    }
    is (s_reading) {
      stateCache := Mux(last, s_end, s_reading)
      entryOff   := Mux(io.mem.resp.fire, entryOff+1.U, entryOff) // axi r fire
    }
    is (s_writing) {
      stateCache := Mux(last, s_end, s_writing)
      entryOff   := Mux(io.mem.req.fire, entryOff+1.U, entryOff) // axi r fire
    }
    is (s_end) {
      stateCache := Mux(hit, s_idle, s_rrq) // if not hit, is come from s_writing, or is come from s_reading.
    }
  }

  // discard the remaining burst transport
  when(io.flush) {
    stateCache := s_idle
    entryOff   := 0.U
  }

  when(last && stateCache ===  s_reading) {
    validArray(replaceWayReg)(setIdx) := true.B // when read the last make it valid
    tagArray(replaceWayReg)(setIdx)   := tag
  } .elsewhen(io.dataWriteBus.req.valid && entryOff === 0.U) { // will write to cache, dirty the cacheline
    validArray(replaceWayReg)(setIdx) := false.B
    tagArray(replaceWayReg)(setIdx)   := 0.U // clear tag!
  }

  val writeCacheAddr = Cat(replaceWayReg, setIdx, entryOff)

  when(io.out.valid && io.out.bits.is_write) {
    dirtyArray(wayIdx)(setIdx) := true.B
  }

  io.in.ready      := hit && (stateCache === s_idle || stateCache === s_end)

  io.out.valid         := hit && io.in.valid // if have no io.in.valid, dcache maybe request repeatedly
  io.out.bits.addr     := io.in.bits.addr
  io.out.bits.is_write := (io.in.bits.cmd === SimpleBusCmd.write_awrite)
  io.out.bits.waddr    := hitCacheAddr
  io.out.bits.wmask    := io.in.bits.wmask
  io.out.bits.wdata    := io.in.bits.wdata << (BYTE_WIDTH.U * byteIdx) // move according to off, so in stage2 we not move.

  io.dataReadBus.req.valid      := (io.in.valid && hit) || MuxLookup(stateCache, false.B)(List(
    s_wrq     -> true.B,
    s_writing -> true.B,
  ))
  io.dataReadBus.req.bits.raddr := MuxLookup(stateCache, hitCacheAddr)(List(
    s_wrq     -> writeCacheAddr,
    s_writing -> writeCacheAddr,
  ))

  // cache missing
  io.mem.req.valid      := MuxLookup(stateCache, false.B)(List(
                                      s_rrq -> true.B,
                                      s_wrq -> true.B,
                                      s_writing -> true.B,
                                      ))
  io.mem.req.bits.cmd   := MuxLookup(stateCache, 0.U)(List(
                                      s_rrq -> SimpleBusCmd.burst_aread,
                                      s_wrq -> SimpleBusCmd.burst_awrite,
                                      s_writing -> SimpleBusCmd.write_burst,
                                      ))
  io.mem.req.bits.addr  := MuxLookup(stateCache, 0.U)(List(
    s_rrq -> ((io.in.bits.addr>>(byteIdxWidth+entryIdxWidth).U)<<(byteIdxWidth+entryIdxWidth).U),
    s_wrq -> Cat(tagArray(replaceWayReg)(setIdx), setIdx, 0.U((byteIdxWidth+entryIdxWidth).W))
  ))
  io.mem.req.bits.len   := (entryCnt-1).U
  io.mem.req.bits.wdata := io.dataReadBus.resp.rdata
  io.mem.req.bits.wmask := "b1111".U
  io.mem.req.bits.wlast := false.B
  io.mem.resp.ready     := MuxLookup(stateCache, false.B)(List(s_reading ->  true.B))

  io.dataWriteBus.req.valid      := (stateCache === s_reading) && io.mem.resp.fire
  io.dataWriteBus.req.bits.waddr := writeCacheAddr
  io.dataWriteBus.req.bits.wdata := io.mem.resp.bits.rdata

  if (EnablePerfCnt) {
    BoringUtils.addSource(WireInit(io.in.fire), perfPrefix+cacheName+"Time")
    // if "io.in.fire" is true in last cycle, it means in this cycle, the new pc will come in, if "hit" is true 
    // in this pc, it means a query is hit.
    // BoringUtils.addSource(WireInit(hit && RegNext(io.in.fire)), perfPrefix+cacheName+"Hit")
    BoringUtils.addSource(WireInit(hit && stateCache === s_end), perfPrefix+cacheName+"Miss")
  }
}

class CacheStage2(val dataWidth: Int, val cacheName: String) extends Module with HasCacheConst {
  val io = IO(new Bundle {
    val in           = Flipped(Decoupled(new Stage1IO)) // from stage 1
    val dataReadBus  = Flipped(new SRAMBundleReadResp(new DataBudle(dataWidth))) // from sram
    val out          = new Stage2IO // to cache.in.resp
    val dataWriteBus = Flipped(CacheDataArrayWriteBus(dataWidth)) // when store inst hit, write to sram, data hit
  })


  io.in.ready            := io.out.resp.ready
  
  io.out.resp.valid      := io.in.valid
  io.out.resp.bits.rdata := Mux(io.in.valid, io.dataReadBus.rdata, 0.U)
  io.out.resp.bits.wresp := false.B
  io.out.addr            := io.in.bits.addr

  io.dataWriteBus.req.valid      := io.in.valid && io.in.bits.is_write
  io.dataWriteBus.req.bits.waddr := io.in.bits.waddr
  io.dataWriteBus.req.bits.wdata := MaskData(io.dataReadBus.rdata, io.in.bits.wdata, MaskExpand(io.in.bits.wmask))
  
  if(cacheName == "dcache") {
    // Debug(io.dataWriteBus.req.valid, "[dcache][stage2], waddr:%x, wdata:%x\n", io.dataWriteBus.req.bits.waddr, io.dataWriteBus.req.bits.wdata)
  }
}

class Cache(val dataWidth: Int, val cacheName: String) extends Module with HasCacheConst {
  val io = IO(new Bundle {
    val in    = Flipped(new SimpleBus) // from ifu, lsu
    val mem   = new SimpleBus // to mem
    val flush = Input(Bool())
    val stage2Addr = Output(UInt(ADDR_WIDTH.W))
  })

  val dataArray = Module(new SRAMTemplate(new DataBudle(dataWidth), addrWidth, cacheName))
  val s1 = Module(new CacheStage1(dataWidth, cacheName))
  val s2 = Module(new CacheStage2(dataWidth, cacheName))

  // s1
  s1.io.in  <> io.in.req
  s1.io.mem <> io.mem
  s1.io.flush := io.flush
  s1.io.dataReadBus  <> dataArray.io.r
  PipelineConnect(s1.io.out, s2.io.in, s2.io.out.resp.fire, io.flush)

  // s2
  s2.io.dataReadBus <> dataArray.io.r.resp

  // dataArray
  val dataWriteArb = Module(new Arbiter(new SRAMBundleWriteReq(new DataBudle(dataWidth), addrWidth), 2))
  s1.io.dataWriteBus.req <> dataWriteArb.io.in(0)
  s2.io.dataWriteBus.req <> dataWriteArb.io.in(1)
  dataWriteArb.io.out <> dataArray.io.w.req

  // cache
  io.stage2Addr := s2.io.out.addr
  io.in.resp    <> s2.io.out.resp
}
