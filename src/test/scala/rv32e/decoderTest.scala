package rv32e

import chisel3._
import chiseltest._
import chisel3.util._
import org.scalatest.flatspec.AnyFlatSpec
import rv32e.config.Configs._
import rv32e.define.Dec_Info._
import rv32e.define.CSR_INFO._
import rv32e.IDU

trait decodeTestFunc {
    
    def imm_i(inst: Long): Long = {
        val signBit = inst >> 31
        val imm = (inst >> 20) & 0xFFF
        if (signBit == 1) {
          imm | 0xFFFFF000L
        } else {
          imm
        }
    }

    def imm_s(inst: Long): Long = {
        val signBit = inst >> 31
        val imm_1 = (inst >> 25) << 5
        val imm_2 = (inst << 20) >> 27
        if (signBit == 1) {
            imm_1 | imm_2 | 0xFFFFF000L
        } else {
            imm_1 | imm_2
        }
    }

    def imm_b(inst: Long): Long = {
        val signBit = inst >> 31
        val imm_11 = (inst >> 7) & 1
        val imm_4to1 = (inst >> 8) & 0xF
        val imm_10to5 = (inst >> 25) & 0x3F
        val imm_12 = (inst >> 31) & 1

        val imm = (imm_12 << 12) | (imm_11 << 11) | (imm_10to5 << 5) | (imm_4to1 << 1)
  
        // Sign extend if the highest bit is set
        if (signBit == 1) {
          imm | 0xFFFFF000L
        } else {
          imm
        } 
    }

    def imm_u(inst: Long): Long = {
        val imm_31to12 = inst >> 12
        imm_31to12 << 12
    }

    def imm_j(inst: Long ): Long = {
        val signBit = inst >> 31
        val imm_20 = (inst >> 31) & 1
        val imm_10to1 = (inst >> 21) & 0x3FF
        val imm_11 = (inst >> 20) & 1
        val imm_19to12 = (inst >> 12) & 0xFF
        val imm_0 = (inst >> 31) & 1

        val imm = (imm_20 << 20) | (imm_10to1 << 1) | (imm_11 << 11) | (imm_19to12 << 12) | (imm_0 << 31)

        // Sign extend if the highest bit is set
        if ( signBit == 1) {
          imm | 0xFFF00000L
        } else {
          imm
        }
    }

    def testFn(dut: IDU): Unit = {

        // val inst = 0x800007b7L
// 
//         dut.io.inst.poke(inst.U)
//         dut.io.out.imm.expect(imm_u(inst).U)
//         dut.io.out.ctrl_sig.alu_op.expect(("b" + alu_add).U)
//         dut.io.out.ctrl_sig.reg_wen.expect(1.U)
//         dut.io.out.ctrl_sig.mem_wen.expect(0.U)
        val inst = 0x30571073L

        dut.io.inst.poke(inst.U)
        dut.io.out.ctrl_sig.csr_op.expect(("b"+csr_csrrw).U)
        dut.io.out.imm.expect(mtvec_id)
        print(dut.decode_info)
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
        test(new IDU) { dut =>
            testFn(dut)
        }
    }
}
