package learn.multiplier

import chisel3._
import chiseltest._
import chisel3.util._
import org.scalatest.flatspec.AnyFlatSpec
import learn.adder.Constants._
import java.util.Random

class booth1Test extends AnyFlatSpec with ChiselScalatestTester {
    val bitWidth = 4
    "booth1" should "pass" in {
        test(new booth1bit(bitWidth)) { dut =>
            dut.io.operandA.poke(5.U)
            dut.io.operandB.poke(5.U)

            dut.reset.poke(true.B) // Assert reset
            dut.clock.step(1)
            dut.reset.poke(false.B) // Deassert reset
            
            dut.clock.step(1)
            dut.clock.step(1)
            dut.clock.step(1)
            dut.clock.step(1)
            dut.io.product.expect(25.U)
        }
    }
}
