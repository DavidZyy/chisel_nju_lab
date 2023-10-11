package rv32e

import chisel3._
import chiseltest._
import chisel3.util._
import org.scalatest.flatspec.AnyFlatSpec
import rv32e.config.Configs._
import rv32e.config.Dec_Info._
import fu.Alu
// import fu.AluTestFunc


trait AluTestFunc {
    def testFn(dut: Alu): Unit = {
        val src1 = 0x80000000L
        val src2 = 0x80000001L

        dut.io.in.src1.poke(src1.U)
//         dut.io.in.src2.poke(src2.U)
//         dut.io.in.alu_op.poke(("b" + alu_sltu).U)
// 
//         dut.io.alu_out.alu_result.expect(1.U)
// 
//         dut.io.in.alu_op.poke(("b" + alu_slt).U)
// 
//         dut.io.alu_out.alu_result.expect(0.U)

        dut.io.in.alu_op.poke(("b" + alu_srl).U)
        dut.io.in.src2.poke(1.U)

        dut.io.alu_out.alu_result.expect(0x40000000L.U)


        dut.io.in.alu_op.poke(("b" + alu_sra).U)
        dut.io.in.src2.poke(1.U)

        dut.io.alu_out.alu_result.expect(0xC0000000L.U)
    }
}

class AluTest extends AnyFlatSpec with ChiselScalatestTester with AluTestFunc {
    "ALU" should "pass" in {
        test(new Alu) { dut =>
            testFn(dut)
        }
    }
}

