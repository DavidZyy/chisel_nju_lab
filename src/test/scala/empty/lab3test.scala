package empty

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class alutester extends AnyFlatSpec with ChiselScalatestTester {
    "alu" should "work" in {
        test(new alu) { dut =>

            // 1+2
            dut.io.A.poke(1.U)
            dut.io.B.poke(2.U)
            dut.io.op.poke("b000".U)
            dut.io.Result.expect(3.U)
            dut.io.zero.expect(0.U)
            dut.io.carry.expect(0.U)
            dut.io.overflow.expect(0.U)

            // 1-2
            dut.io.A.poke(1.U)
            dut.io.B.poke(2.U)
            dut.io.op.poke("b001".U)
            dut.io.Result.expect(15.U)
            dut.io.zero.expect(0.U)
            dut.io.carry.expect(1.U)
            // printf("Subtraction: %d\n", dut.sub)
            // dut.io.carry.expect(0.U)
            dut.io.overflow.expect(0.U)

            // 0-1
            dut.io.A.poke(0.U)
            dut.io.B.poke(1.U)
            dut.io.op.poke("b001".U)
            dut.io.Result.expect(15.U)
            dut.io.zero.expect(0.U)
            dut.io.carry.expect(1.U)
            // dut.io.carry.expect(0.U)
            dut.io.overflow.expect(0.U)

            // 7 - -1
            dut.io.A.poke(7.U)
            dut.io.B.poke(15.U)
            dut.io.op.poke("b001".U)
            dut.io.Result.expect(8.U)
            dut.io.zero.expect(0.U)
            dut.io.carry.expect(1.U)
            dut.io.overflow.expect(1.U)

        }
    }
}
