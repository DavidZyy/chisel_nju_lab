package empty
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class shift_reg_test extends AnyFlatSpec with ChiselScalatestTester {
    "shiftreg" should "work" in {
        test(new shift_reg) { dut =>

            for(i <- 1 until 8) {
                dut.io.in.poke(1.U)
                dut.io.op.poke("b101".U)
                dut.clock.step(1)
                dut.io.out.expect(0.U)
            }
            // dut.io.out.expect(1.U)

            dut.io.in.poke(1.U)
            dut.io.op.poke("b101".U)
            dut.clock.step(1)
            dut.io.out.expect("b11111111".U)
        }
    }
}

class barrel_shift_test extends AnyFlatSpec with ChiselScalatestTester {
    "barrel_shift" should "work" in {
        test(new barrel_shift) { dut =>

            dut.io.din.poke("b11111111".U)
            dut.io.shamt.poke(4.U)
            dut.io.L_R.poke(true)
            dut.io.A_L.poke(true)
            
            dut.io.dout.expect("b11110000".U)

            dut.io.L_R.poke(false)

            dut.io.dout.expect("b11111111".U)

            dut.io.A_L.poke(false)
            dut.io.dout.expect("b00001111".U)

        }
    }
}

class LFSR_test extends AnyFlatSpec with ChiselScalatestTester {
    "lfsr" should "work" in {
        test(new LFSR) { dut =>
            dut.io.dout.expect("b00000001".U)

            dut.clock.step(1)
            dut.io.dout.expect("b10000000".U)

            dut.clock.step(1)
            dut.io.dout.expect("b01000000".U)

            dut.clock.step(1)
            dut.io.dout.expect("b00100000".U)

            dut.clock.step(1)
            dut.io.dout.expect("b00010000".U)

            dut.clock.step(1)
            dut.io.dout.expect("b10001000".U)
        }
    }
}