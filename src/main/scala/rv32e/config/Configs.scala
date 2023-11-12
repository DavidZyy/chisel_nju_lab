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

object Cache_Configs {
  val offWidth    = 5
  val idxWidth    = 5
  val tagWidth    = Configs.ADDR_WIDTH-offWidth-idxWidth

//   val blkWidth    = (1<<offWidth)*Configs.BYTE_WIDTH

  val off_LSB     = 0
  val off_MSB     = off_LSB + offWidth - 1
  val idx_LSB     = off_MSB + 1
  val idx_MSB     = idx_LSB + idxWidth - 1
  val tag_LSB     = idx_MSB + 1
  val tag_MSB     = tag_LSB + tagWidth - 1

  val numSetsWidth = 1
  val numSets      = 1<<numSetsWidth
  val numCacheLine = 1<<idxWidth // each set of cache has cache lines
  val numEnts      = 1<<offWidth // each cache line has entrys
}

object Axi_Configs {
  val BRESP_WIDTH = 2
  val RRESP_WIDTH = 2
  val WSTRB_WIDTH = Configs.DATA_WIDTH / Configs.BYTE_WIDTH

}
