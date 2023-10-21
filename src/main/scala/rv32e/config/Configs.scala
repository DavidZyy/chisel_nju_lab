package rv32e.config

import chisel3._

object Configs {
    // for addr
    val ADDR_WIDTH      =   32
    val ADDR_BYTE_WIDTH =   4

    // for regs and imm
    val DATA_WIDTH      =   32
    val BYTE_WIDTH      =   8
    val INST_WIDTH      =   32
    val INST_BYTE_WIDTH =   4

    val MEM_INST_SIZE   =   1024
    val MEM_DATA_SIZE   =   2048 

    val START_ADDR: Long =  0x80000000L

    /* 2^4 = 16 general registers */
    val REG_OP_WIDTH = 5
}

