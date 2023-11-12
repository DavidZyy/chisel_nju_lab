package learn

import chisel3._
import chisel3.util._
import chisel3.tester._
import chisel3.tester.RawTester.test
import rv32e.config.Cache_Configs._
import rv32e.config.Configs._

class ReadWriteSmem extends Module {
  val width: Int = 16
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val write = Input(Bool())
    val w_en = Input(Bool())
    val addr = Input(UInt(10.W))
    val dataIn = Input(UInt(width.W))
    val dataOut = Output(UInt(width.W))
  })

  val mask = Fill(512, io.write).asBools
  val vmask = VecInit(mask)
  // val mask = 
  // val writeMask: Seq[Bool] = (0 until width).map(i => i % 2 == 0)
  val mem = SyncReadMem(512, UInt(width.W))
  // Create one write port and one read port
  when(io.w_en) {
    mem.write(io.addr, io.dataIn)
  }
  io.dataOut := mem.read(io.addr, io.enable)
}


object mem_main extends App {
    emitVerilog(new ReadWriteSmem(), Array("--target-dir", "generated")) // emitVerilog(new WriteSmem(), Array("--target-dir", "generated"))
}

// cache test
class RWSmem extends Module {
  val io = IO(new Bundle {
    val enable  = Input(Bool())
    val write   = Input(Bool())
    val w_en    = Input(Bool())
    val addr    = Input(UInt(ADDR_WIDTH.W))
    val dataIn  = Input(UInt(DATA_WIDTH.W))
    val dataOut = Output(UInt(DATA_WIDTH.W))
  })

  val EntId       = io.addr(off_MSB, off_LSB)
  val CacheLineId = io.addr(idx_MSB, idx_LSB)
  val tagId       = io.addr(tag_MSB, tag_LSB)
  val SetId = RegInit(0.U(numSetsWidth.W))
  SetId := SetId +1.U
  // val mem = SyncReadMem(1024, UInt(width.W))
  // val mem = SyncReadMem(1024, Vec(4, UInt(width.W)))

  // val temp = Vec(numEnts, UInt(DATA_WIDTH.W))
  // val temp = VecInit(Seq.fill(numEnts)(0.U(DATA_WIDTH.W)))
  // val temp = Reg(VecInit(Seq.fill(numCacheLine)(VecInit(Seq.fill(numEnts)(0.U(DATA_WIDTH.W))))))

  // val mem = SyncReadMem(numSets, Vec(numCacheLine, Vec(numEnts, UInt(DATA_WIDTH.W))))
  // val mem = Reg(Vec(numSets, Vec(numCacheLine, Vec(numEnts, UInt(DATA_WIDTH.W)))))
  // val mem = SyncReadMem(numSets, Vec(numCacheLine, temp))
  // val mem = SyncReadMem(numSets, temp)

  val mem = RegInit(VecInit(Seq.fill(numSets)(VecInit(Seq.fill(numCacheLine)(VecInit(Seq.fill(numEnts)(0.U(DATA_WIDTH.W))))))))
  // val mem = RegInit(Vec(numSets, Vec(numCacheLine, VecInit(Seq.fill(numEnts)(0.U(DATA_WIDTH.W))))))
  io.dataOut := DontCare
  // mem(chosen)(idx)(off)
  when(io.enable) {
    val rdwrPort = mem(SetId)(CacheLineId)(EntId)
    when (io.write) { rdwrPort := io.dataIn }
      .otherwise    { io.dataOut := rdwrPort }
  }
}

object mem2_main extends App {
    emitVerilog(new RWSmem(), Array("--target-dir", "generated"))
    // emitVerilog(new WriteSmem(), Array("--target-dir", "generated"))
}

object mem5_main extends App {
    emitVerilog(new RWSmem(), Array("--target-dir", "generated"))
    // emitVerilog(new WriteSmem(), Array("--target-dir", "generated"))
}

// cache 1
class ReadWriteSmemV extends Module {
  val io = IO(new Bundle {
    val enable  = Input(Bool())
    val write   = Input(Bool())
    val w_en    = Input(Bool())
    val addr    = Input(UInt(ADDR_WIDTH.W))
    val dataIn  = Input(UInt(DATA_WIDTH.W))
    val dataOut = Output(UInt(DATA_WIDTH.W))
  })

  val random_num = RegInit(0.U(numSetsWidth.W))
  random_num := random_num+1.U
  val data = VecInit(Seq.fill(4)(io.dataIn))
  // val mem = SyncReadMem(12, Vec(4, UInt(2.W)))
  val mem = SyncReadMem(12, Vec(4, Vec(4, UInt(2.W))))

  val mask = MuxLookup(random_num, 0.U, List(
    0.U -> "b0001".U,
    1.U -> "b0010".U,
    2.U -> "b0100".U,
    3.U -> "b1000".U,
  ))

  // mem.write(io.addr, data, mask.asBools)

  val rdata = mem.read(io.addr, io.enable)
  // we coulde use map here to simplify the code
  io.dataOut := MuxLookup(random_num, 0.U, List(
    0.U -> rdata(0),
    1.U -> rdata(1),
    2.U -> rdata(2),
    3.U -> rdata(3),
  ))
}

object mem3_main extends App {
    emitVerilog(new ReadWriteSmemV(), Array("--target-dir", "generated"))
}

class ReadWriteSmemV2 extends Module {
  val width: Int = 16
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val write = Input(Bool())
    val w_en = Input(Bool())
    val addr = Input(UInt(10.W))
    val dataIn = Input(UInt(width.W))
    val dataOut = Output(UInt(2.W))
  })

//   val data = VecInit(Seq.fill(4)(io.dataIn))
//   val mask = Fill(4, "b1".U)
// 
//   val mem = SyncReadMem(12, Vec(4, UInt(2.W)))
//   mem.write(io.addr, data, mask.asBools)
// 
//   val rdata = mem.read(io.addr, io.enable)
//   io.dataOut := Cat(rdata(0), rdata(1), rdata(2), rdata(3))

  // val mem = VecInit(Seq.fill(4)(SyncReadMem(64, UInt(64.W))))
  val smem = Seq.fill(4)(SyncReadMem(64, UInt(64.W)))
  // val mem = VecInit(smem)

}


class Cachetry extends Module {
  val io = IO(new Bundle {
    val read = Input(Bool())
    val write = Input(Bool())
    val address = Input(UInt(6.W))
    val dataIn = Input(UInt(32.W))
    val dataOut = Output(UInt(32.W))
  })

  val numSets   = 4
  val numEnts   = 64
  val tagWidth  = 4
  val dataWidth = 32

  val tagArray   = Seq.fill(numSets)(Mem(numEnts, UInt(tagWidth.W)))
  val dataArray  = Seq.fill(numSets)(Mem(numEnts, UInt(dataWidth.W)))
  val validArray = Seq.fill(numSets)(Mem(numEnts, Bool()))

  val hit = Wire(Bool())

  val setIndex = io.address(7, 4)
  val tag = io.address(3, 0)

  val ramdom = RegInit(0.U)
  ramdom := ramdom+1.U

  val valid    = validArray(ramdom.toString().toInt).read(io.address)
  val tagMatch = tagArray(ramdom.toString().toInt).read(io.address) === tag

  hit := io.read && valid && tagMatch

  when(io.write) {
    tagArray  (ramdom.toString().toInt).write(io.address, tag)
    dataArray (ramdom.toString().toInt).write(io.address, io.dataIn)
    validArray(ramdom.toString().toInt).write(io.address, true.B)
  }

  io.dataOut := Mux(hit, dataArray(ramdom.toString().toInt).read(io.address(1, 0)), 0.U)
}


object mem4_main extends App {
    emitVerilog(new Cachetry(), Array("--target-dir", "generated"))
    // emitVerilog(new WriteSmem(), Array("--target-dir", "generated"))
}




class WriteSmem extends Module {
  val width: Int = 32
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val write = Input(Bool())
    val addr = Input(UInt(10.W))
    val dataIn = Input(UInt(width.W))
    val dataOut = Output(UInt(width.W))
  })

  val mem = Mem(1024, UInt(width.W))
  // Create one write port and one read port
  mem.write(io.addr, io.dataIn)
//   io.dataOut := mem.read(io.addr, io.enable)
  io.dataOut := mem.read(io.addr)
}


class sin extends Module {
  val Pi = math.Pi
  def sinTable(amp: Double, n: Int) = {
    val times =
      (0 until n).map(i => (i*2*Pi)/(n.toDouble-1) - Pi)
    val inits =
      times.map(t => Math.round(amp * math.sin(t)).asSInt(32.W))
    VecInit(inits)
  }
}

