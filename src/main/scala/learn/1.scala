package learn

import chisel3._
import chisel3.util._
import scala.math._

object aamain  extends App {
    val m = VecInit(1.U, 2.U, 4.U, 8.U)
    val c = Counter(m.length)
    c.inc()
    val r = m(c.value)
    // println("hello!")
}

object main extends App {
//     Usage
//     val myObject = new MyClass()
//     myObject.printSharedString()
// 
//     AnotherObject.printSharedString()
  // println( MyEnum.StateA)
  // println( MyEnum.StateB)
  // println( MyEnum.StateC)

  // println("Hello, world! this is learn !")
  val Pi = math.Pi
  def sinTable(amp: Double, n: Int) = {
    val times =
      (0 until n).map(i => (i*2*Pi)/(n.toDouble-1) - Pi)
    val inits =
      times.map(t => Math.round(amp * math.sin(t)).asSInt(32.W))
    // VecInit(inits)
    println(inits)
  }

  sinTable(2, 10)
  
}


object yield_main extends App {
  val hitarray = for (i <- 0 until 100) yield {
    i
  }
  println(hitarray)
}

object log_main extends App {
  val number = 16
  val logBase2 = log(number) / log(2)

  println(logBase2)
}

object addmain extends App {
  val devAddrSpace = List(
    (0x40600000L, 0x10L), // uart
    (0x50000000L, 0x400000L), // vmem
    (0x40001000L, 0x8L),  // vga ctrl
    (0x40000000L, 0x1000L),  // flash
    (0x40002000L, 0x1000L), // dummy sdcard
    (0x40004000L, 0x1000L), // meipGen
    (0x40003000L, 0x1000L)  // dma
  )
  
  val addr = 0x1L
  val outMatchVec = devAddrSpace.map(
    range => (addr >= range._1 && addr < (range._1 + range._2)))
  println(outMatchVec)
}

// object MaskExpand {
//  def apply(m: UInt) = Cat(m.asBools.map(Fill(8, _)).reverse)
// }

// class pand extends Module {
//   val io = IO(new Bundle {
//     val in  = Input(UInt(4.W))
//     val out = Output(UInt(32.W))
//   })
// 
//   io.out := MaskExpand(io.in)
// }

// object pand_main extends App {
//     emitVerilog(new pand(), Array("--target-dir", "generated"))
// }

