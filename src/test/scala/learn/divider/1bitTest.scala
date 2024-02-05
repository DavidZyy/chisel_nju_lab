package learn.divider

import chisel3._
import chisel3.util._
import chiseltest._

import org.scalatest.flatspec.AnyFlatSpec

import learn.adder.Constants._

import java.util.Random
import treadle2.executable.Big

class divider0Test extends AnyFlatSpec with ChiselScalatestTester {
    val bitWidth = 64
    "booth2" should "pass" in {
        test(new divider0(bitWidth)) { dut =>
            for(i <- 0 until 100) {
                // val A = BigInt(1)
                // val B = BigInt(-1)
                val A = BigInt(bitWidth, new Random) - (BigInt(1) << (bitWidth - 1))
                var B = BigInt(bitWidth, new Random) - (BigInt(1) << (bitWidth - 1))
                while (B == BigInt(0)) {
                    B = BigInt(bitWidth, new Random) - (BigInt(1) << (bitWidth - 1))
                }

                val quotient = A / B // should be the complete of -6
                val remainder = A % B

                val complementA: BigInt =
                if (A >= BigInt(0)) { A }
                else { A + (BigInt(1) << bitWidth) }
                val complementB: BigInt =
                if (B >= BigInt(0)) { B }
                else { B + (BigInt(1) << bitWidth) }
                val complementQuotient: BigInt =
                if (quotient >= BigInt(0)) {quotient}
                else (quotient + (BigInt(1) << bitWidth))
                val complementRemainder: BigInt =
                if (remainder >= BigInt(0)) {remainder}
                else (remainder + (BigInt(1) << bitWidth))

                println("A: " + A.toString(16) + " " + A)
                println("B: " + B.toString(16) + " " + B)
                println("quotient: "  + quotient.toString(16) + " " + quotient)
                println("remainder: " + remainder.toString(16) + " " + remainder)
                println("complementA: " + complementA.toString(16) + " " + complementA)
                println("complementB: " + complementB.toString(16) + " " + complementB)
                println("complementQuotient: "  + complementQuotient.toString(16) + " " + complementQuotient)
                println("complementRemainder: " + complementRemainder.toString(16) + " " + complementRemainder)
                println() // '\n'

                dut.io.operandA.poke(complementA.U)
                dut.io.operandB.poke(complementB.U)

                dut.reset.poke(true.B) // Assert reset
                dut.clock.step(1)
                dut.reset.poke(false.B) // Deassert reset

                // need bitWidth times iteration
                // quotioent is bitWidth, 1 iteration
                // get 1 bit of quotient.
                dut.clock.step(bitWidth)

                dut.io.quotient.expect(complementQuotient.U)
                dut.io.remainder.expect(complementRemainder.U)
            }
        }
    }
}