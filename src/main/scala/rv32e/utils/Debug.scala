package rv32e.utils

import chisel3._
import chisel3.util._

/**
  * NOTE here!!
  * The the printf is compiled to fwrite in verilog, 
  * and the fwrite in verilog codes is compiled by verilator to stderr, so if you want to redirect the info to file,
  * you should use "2>" not just ">".
  */

object LogUtil {
    def apply(cond: Bool, pable: Printable): Any = {
        val commonInfo = p"[${GTimer()}]: "
        when(cond) {
            printf(commonInfo)
            printf(pable)
        }
    }
    
    def apply(time: UInt, cond: Bool, pable: Printable): Any = {
        val commonInfo = p"[${GTimer()-time}]: "
        when(cond) {
            printf(commonInfo)
            printf(pable)
        }
    }
}

object Debug {
    def apply(fmt: String, data: Bits*): Any = apply(true.B, fmt, data:_*)
    def apply(cond: Bool, fmt: String, data: Bits*): Any = apply(cond, Printable.pack(fmt, data:_*))
    def apply(cond: Bool, pable: Printable): Any = LogUtil(cond, pable)


    def apply(time: UInt, cond: Bool, fmt: String, data: Bits*): Any = apply(time, cond, Printable.pack(fmt, data:_*))
    def apply(time: UInt, cond: Bool, pable: Printable): Any = LogUtil(time, cond, pable)
}

