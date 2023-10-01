package rv32e.fu

import chisel3._
import chiseltest._
import chisel3.util._
import org.scalatest.flatspec.AnyFlatSpec
import rv32e.config.Configs._
import rv32e.config.Dec_Info._
import rv32e.config.CSR_INFO._
import rv32e.EXU.CSR


trait CSRTestFunc {
    def testFn(dut: CSR): Unit = {
        // test ecall
//         dut.io.in.csr_op.poke(("b"+csr_ecall).U)
//         dut.io.in.cur_pc.poke(0x80000009L.U)
//         dut.io.in.csr_id.poke((0x342).U)
//         dut.io.in.wdata.poke((0x11111111).U)
// 
//         dut.io.out.ctrl_csr.expect(true.B)
//         dut.io.out.csr_pc.expect(0.U)
//         dut.io.out.r_csr.expect(0.U)
// 
//         dut.clock.step(1)
// 
//         dut.io.in.csr_op.poke(("b"+csr_mret).U)
//         dut.io.out.csr_pc.expect(0x80000009L.U)

        // test csrrw
        dut.io.in.csr_op.poke(("b"+csr_csrrw).U)
        dut.io.in.cur_pc.poke(0x80000decL.U)
        dut.io.in.csr_id.poke(mtvec_id)
        dut.io.in.wdata.poke((0x80000e08L).U)

        dut.clock.step(1)

        // dut.io.out.difftest.mtvec.expect((0x80000e08L).U)
        dut.io.out.difftest.mtvec.expect((0x80000e08L).U)

    }

}

class CSRTest extends AnyFlatSpec with ChiselScalatestTester with CSRTestFunc {
    "CSR" should "pass" in {
        test(new CSR) { dut =>
            testFn(dut)
        }
    }
}
