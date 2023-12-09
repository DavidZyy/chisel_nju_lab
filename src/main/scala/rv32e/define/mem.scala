package rv32e.define

/**
  * used for memory and mmio
  */
object Mem {
    val pmemBase  = 0x80000000L
    val pmemSize  = 0x8000000L
    val mmioBase  = 0xa0000000L
    val mmioSize  = 0x1200000L
}