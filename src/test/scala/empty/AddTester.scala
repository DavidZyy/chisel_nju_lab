/*
 * Dummy tester to start a Chisel project.
 *
 * Author: Martin Schoeberl (martin@jopdesign.com)
 * 
 */

package empty

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import empty.{multiplexer, encoder_prio83}

class AddTester extends AnyFlatSpec with ChiselScalatestTester {

  "Add" should "work" in {
    test(new multiplexer) { dut =>
      for (a <- 0 to 2) {
        for (b <- 0 to 3) {
          val result = a + b
          // dut.io.a.poke(a.U)
          // dut.io.b.poke(b.U)
          // dut.clock.step(1)
          // dut.io.c.expect(result.U)
        }
      }
    }
  }
}

class en_prio83tester extends AnyFlatSpec with ChiselScalatestTester {
  "Add" should "work" in {
    test(new encoder_prio83) { dut =>
      // dut.io.en.poke(0.U)
      dut.io.en.poke(1.U)

      dut.io.x.poke("b10000100".U)
      dut.io.y.expect(7.U)

      dut.io.x.poke("b01000100".U)
      dut.io.y.expect(6.U)

      dut.io.x.poke("b00100100".U)
      dut.io.y.expect(4.U)
      // dut.io.y.expect(0.U)
    }
  }
}

