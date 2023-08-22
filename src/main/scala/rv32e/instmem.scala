package rv32e

import chisel3._
import chisel3.util._

import rv32e.config.Configs._

import chisel3.util.experimental.loadMemoryFromFile
import firrtl.annotations.MemoryLoadFileType

class MemInstIO extends Bundle {
    val addr = Input(UInt(ADDR_WIDTH.W))    // 指令地址
    val inst = Output(UInt(INST_WIDTH.W))   // 指令输出
}

class RomBB extends BlackBox with HasBlackBoxResource {
    val io = IO(new Bundle {
        // val clock = Input(Clock())
        val addr = Input(UInt(ADDR_WIDTH.W))
        val inst = Output(UInt(ADDR_WIDTH.W))
    })

    addResource("/RomBB.v")
}

class Rom extends Module {
    val io = IO(new MemInstIO())    // 输入输出接口

    // 指令内存，能存放MEM_INST_SIZE条INST_WIDTH位的指令
    // val mem = Mem(MEM_INST_SIZE, UInt(INST_WIDTH.W))
    // val true_addr = io.addr - START_ADDR.U

    // io.inst := mem.read(true_addr >> 2)    // 读取对应位置的指令并输出
    val RomBB_i1 = Module(new RomBB())

    // RomBB_i1.io.clock := clock
    RomBB_i1.io.addr := io.addr

    io.inst := RomBB_i1.io.inst
}
