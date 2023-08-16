package learn

import chisel3._
import chiseltest._
import chisel3.util._
import org.scalatest.flatspec.AnyFlatSpec

trait decodeTestFunc {
    def testFn(dut: SimpleDecoder): Unit = {
        dut.input.poke(1.U)
        dut.output.expect(0.U)

        dut.input.poke(5.U)
        dut.output.expect(1.U)

        dut.input.poke(7.U)
        dut.output.expect(1.U)

        // dut.input.poke(1.U)
    }
}

// trait decodeTestFunc {
//     def testFn(dut: SimpleDecoder): Unit = {
//         // Test all possible inputs and check the output
//         for (i <- 0 until 8) {
//             dut.input.poke(i.U)
//             i match {
//                 case 4 | 5 | 7 => dut.output.expect(1.U) // Defined outputs
//                 case _ => dut.output.expect(1.U) // Undefined or default outputs
//             }
//         }
//     }
// }

class decodeTest extends AnyFlatSpec with ChiselScalatestTester with decodeTestFunc {
    "decode" should "pass" in {
        test(new SimpleDecoder) { dut =>
            testFn(dut)
        }
    }
}