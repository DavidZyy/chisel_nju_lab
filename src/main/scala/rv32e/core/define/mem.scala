package rv32e.core.define

/**
  * used for memory and mmio
  */
object Mem {
    val pmemBase  = 0x80000000L
    val pmemSize  = 0x8000000L
    val mmioBase  = 0xa0000000L
    val mmioSize  = 0x1200000L

    val MMIO_BASE   = mmioBase
    val DEVICE_BASE = mmioBase

    val SERIAL_PORT     = (DEVICE_BASE + 0x00003f8L)
    val KBD_ADDR        = (DEVICE_BASE + 0x0000060L)
    val RTC_ADDR        = (DEVICE_BASE + 0x0000048L)
    val VGACTL_ADDR     = (DEVICE_BASE + 0x0000100L)
    val AUDIO_ADDR      = (DEVICE_BASE + 0x0000200L)
    val DISK_ADDR       = (DEVICE_BASE + 0x0000300L)
    val FB_ADDR         = (MMIO_BASE   + 0x1000000L)
    val AUDIO_SBUF_ADDR = (MMIO_BASE   + 0x1200000L)
 
    val RTCSize         = 0x8L
}