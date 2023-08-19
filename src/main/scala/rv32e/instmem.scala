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

class Rom extends Module {
    val io = IO(new MemInstIO())    // 输入输出接口

    // 指令内存，能存放MEM_INST_SIZE条INST_WIDTH位的指令
    val mem = Mem(MEM_INST_SIZE, UInt(INST_WIDTH.W))

    io.inst := mem.read(io.addr >> 2)    // 读取对应位置的指令并输出
}
