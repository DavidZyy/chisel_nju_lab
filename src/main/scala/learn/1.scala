package learn

import chisel3._
import chisel3.util.Counter
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