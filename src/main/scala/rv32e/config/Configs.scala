package rv32e.config

import chisel3._
import scala.math._

object Configs {
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
    val REG_OP_WIDTH = 5
}

object Cache_Configs {
  val offWidth = 4
  val idxWidth = 4
  val tagWidth = Configs.ADDR_WIDTH-offWidth-idxWidth

//   val blkWidth    = (1<<offWidth)*Configs.BYTE_WIDTH

  val byte_LSB = 0
  val byte_MSB = byte_LSB + Configs.DATA_BYTE_WIDTH - 1
  val ent_LSB  = byte_MSB + 1
  val ent_MSB  = ent_LSB  + (offWidth-Configs.DATA_BYTE_WIDTH) - 1
  val idx_LSB  = ent_MSB  + 1
  val idx_MSB  = idx_LSB  + idxWidth - 1
  val tag_LSB  = idx_MSB  + 1
  val tag_MSB  = tag_LSB  + tagWidth - 1

  val numSetsWidth = 1
  val numSets      = 1<<numSetsWidth
  val numCacheLine = 1<<idxWidth // each set of cache has cache lines
  val numEnts      = (1<<offWidth)/Configs.DATA_BYTE // each cache line has entrys
}

object Axi_Configs {
  val BRESP_WIDTH   = 1
  val RRESP_WIDTH   = 1
  val WSTRB_WIDTH   = Configs.DATA_BYTE
  val AxSIZE_WIDTH  = 3 // AWSIZE and ARSIZE
  val AxLEN_WIDTH   = 8
  val AxBURST_WIDTH = 2
  val FIXED = 0
  val INCR  = 1
  val WRAP  = 2
}
