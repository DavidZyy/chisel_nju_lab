package rv32e.core

import chisel3._
import scala.math._

object config {
    val BYTE_WIDTH      =   8

    val ADDR_WIDTH      =   32
    val DATA_WIDTH      =   32
    val INST_WIDTH      =   32

    val ADDR_BYTE       =   ADDR_WIDTH / BYTE_WIDTH
    val DATA_BYTE       =   DATA_WIDTH / BYTE_WIDTH
    val INST_BYTE       =   INST_WIDTH / BYTE_WIDTH

    val DATA_BYTE_WIDTH = (log(DATA_BYTE) / log(2)).toInt

    val START_ADDR: Long =  0x80000000L

    /* 2^4 = 16 general registers */
    val REG_OP_WIDTH    = 5

    val wmaskWidth      = DATA_WIDTH / BYTE_WIDTH


}
