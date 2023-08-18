package rv32e

import chisel3._
import chiseltest._
import chisel3.util._
import org.scalatest.flatspec.AnyFlatSpec
import rv32e.config.Configs._
import rv32e.config.Dec_Info._

trait RamTestFunc {
    def testFn(dut: Ram): Unit = {
        // store
        dut.io.ram_in.mem_wen.poke(1.U)
        // sb write in 0x87654321
        dut.io.ram_in.lsu_op.poke(("b" + lsu_sb).U)

        dut.io.ram_in.addr.poke(0.U)
        dut.io.ram_in.wdata.poke(0x00000021L.U)
        dut.clock.step(1)
        dut.io.ram_in.addr.poke(1.U)
        dut.io.ram_in.wdata.poke(0x00000043L.U)
        dut.clock.step(1)
        dut.io.ram_in.addr.poke(2.U)
        dut.io.ram_in.wdata.poke(0x00000065L.U)
        dut.clock.step(1)
        dut.io.ram_in.addr.poke(3.U)
        dut.io.ram_in.wdata.poke(0x00000087L.U)
        dut.clock.step(1)
        
        // sh write in 0x87654321
        dut.io.ram_in.lsu_op.poke(("b" + lsu_sh).U)

        dut.io.ram_in.addr.poke(4.U)
        dut.io.ram_in.wdata.poke(0x00004321L.U)
        dut.clock.step(1)
        dut.io.ram_in.addr.poke(6.U)
        dut.io.ram_in.wdata.poke(0x00008765L.U)
        dut.clock.step(1)

        // sw write in 0x87654321
        dut.io.ram_in.lsu_op.poke(("b" + lsu_sw).U)
        dut.io.ram_in.addr.poke(8.U)
        dut.io.ram_in.wdata.poke(0x87654321L.U)
        dut.clock.step(1)

        // load
        dut.io.ram_in.mem_wen.poke(0.U)
        // lb
        dut.io.ram_in.lsu_op.poke(("b" + lsu_lb).U)

        dut.io.ram_in.addr.poke(0.U)
        dut.io.ram_out.rdata.expect(0x00000021L.U)
        dut.io.ram_in.addr.poke(1.U)
        dut.io.ram_out.rdata.expect(0x00000043L.U)
        dut.io.ram_in.addr.poke(2.U)
        dut.io.ram_out.rdata.expect(0x00000065L.U)
        dut.io.ram_in.addr.poke(3.U)
        dut.io.ram_out.rdata.expect(0xffffff87L.U)

        // lbu
        dut.io.ram_in.lsu_op.poke(("b" + lsu_lbu).U)
        dut.io.ram_in.addr.poke(3.U)
        dut.io.ram_out.rdata.expect(0x00000087L.U)

        // lh
        dut.io.ram_in.lsu_op.poke(("b" + lsu_lh).U)
        dut.io.ram_in.addr.poke(4.U)
        dut.io.ram_out.rdata.expect(0x00004321L.U)
        dut.io.ram_in.addr.poke(6.U)
        dut.io.ram_out.rdata.expect(0xffff8765L.U)

        // lhu
        dut.io.ram_in.lsu_op.poke(("b" + lsu_lhu).U)
        dut.io.ram_in.addr.poke(6.U)
        dut.io.ram_out.rdata.expect(0x00008765L.U)

        // lw
        dut.io.ram_in.lsu_op.poke(("b" + lsu_lw).U)

        dut.io.ram_in.addr.poke(0.U)
        dut.io.ram_out.rdata.expect(0x87654321L.U)
        dut.io.ram_in.addr.poke(4.U)
        dut.io.ram_out.rdata.expect(0x87654321L.U)
        dut.io.ram_in.addr.poke(8.U)
        dut.io.ram_out.rdata.expect(0x87654321L.U)
    }
}

// class RamTest extends 
class RamTest extends AnyFlatSpec with ChiselScalatestTester with RamTestFunc {
    "Ram" should "pass" in {
        test(new Ram) { dut =>
            testFn(dut)
        }
    }
}