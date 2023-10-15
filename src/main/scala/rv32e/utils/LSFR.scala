package rv32e.utils

import chisel3._
import chisel3.util._

class LFSR extends Module {
  val io = IO(new Bundle {
    val out = Output(UInt(4.W))
  })

  val reg = RegInit(1.U(4.W)) // Initialize the LFSR with a non-zero value

  // Define the LFSR feedback polynomial.
  // For a 4-bit LFSR, we'll use the polynomial x^4 + x^3 + 1, which corresponds to taps at bit positions 4 and 3.
  val feedback = reg(3) ^ reg(0)

  // Connect the LFSR feedback to the input of the shift register.
  reg := Cat(feedback, reg(3, 1))

  io.out := reg
}

// object LFSRMain extends App {
//   chisel3.Driver.execute(args, () => new LFSR)
// }
