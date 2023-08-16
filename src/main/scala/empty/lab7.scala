package empty

import chisel3._
import chisel3.util._

// object FSM_state extends Enumeration {
//   val S0, S1, S2, S3, S4, S5, S6, S7, S8 = Value
// }

class FSM extends Module {
    val io = IO(new Bundle {
        val in  =   Input(Bool())
        val out =   Output(Bool())
    })

    val S0  = 0.U
    val S1  = 1.U
    val S2  = 2.U
    val S3  = 3.U
    val S4  = 4.U
    val S5  = 5.U
    val S6  = 6.U
    val S7  = 7.U
    val S8  = 8.U

    val reg             =   RegInit(UInt(4.W), 0.U)
    val current_state   =   WireInit(S0)
    val next_state      =   WireInit(S0)

    reg           := next_state
    current_state := reg

    next_state  := MuxLookup(current_state, S0, Seq(
        S0 -> Mux(io.in, S5, S1),
        S1 -> Mux(io.in, S5, S2),
        S2 -> Mux(io.in, S5, S3),
        S3 -> Mux(io.in, S5, S4),
        S4 -> Mux(io.in, S5, S4),
        S5 -> Mux(io.in, S6, S1),
        S6 -> Mux(io.in, S7, S1),
        S7 -> Mux(io.in, S8, S1),
        S8 -> Mux(io.in, S8, S1)
    ))

    val out = MuxLookup(current_state, false.B, Seq(
        S0 -> false.B,
        S1 -> false.B,
        S2 -> false.B,
        S3 -> false.B,
        S4 -> true.B,
        S5 -> false.B,
        S6 -> false.B,
        S7 -> false.B,
        S8 -> true.B
    ))
    io.out := out
}

object FSM_main extends App {
  val rela_path: String = "lab7/FSM/vsrc"
  println("Generating the FSM hardware")
  emitVerilog(new FSM(), Array("--target-dir", Path.path + rela_path))
}

// class ps2_keyboard extends Module {
//     val io = IO(new Bundle {
//         val ps2_clk     =   Input(Bool())
//         val ps2_data    =   Input(Bool())
//         val nextdata_n  =   Input(Bool())
//         val data        =   Output(UInt(8.W))
//         val ready       =   Output(Bool())
//         val overflow    =   Output(Bool())
//     })
// 
//     // reg
//     // val buffer          =   RegInit(UInt(10.W), 0.U)
//     val buffer          =   RegInit(UInt(10.W), 0.U)
//     // val buffer          =   RegInit(UInt(1.W), 0.U)
//     val fifo            =   RegInit(VecInit(Seq.fill(8)(0.U(8.W))))
//     // val fifo            =   RegInit(Vec(8, UInt(8.W)), 0.U)
//     // val fifo            =   Reg(Vec(8, UInt(8.W)))
//     val w_ptr           =   RegInit(UInt(3.W), 0.U)
//     val r_ptr           =   RegInit(UInt(3.W), 0.U)
//     val count           =   RegInit(UInt(4.W), 0.U)
//     val ps2_clk_sync    =   RegInit(UInt(3.W), 0.U)
// 
//     // wire
//     // val sampling        =   WireInit(UInt(1.W), 0.U)
//     val sampling        =   WireInit(false.B)
//     val ready_wire      =   WireInit(false.B)
// 
//     ps2_clk_sync    :=  Cat(ps2_clk_sync(1,0), io.ps2_clk)
// 
//     sampling        :=  ps2_clk_sync(2) & ~ps2_clk_sync(1)
// 
//     // Initialize overflow to false.B
//     io.overflow := false.B
// 
//     // val xorResult = Wire(UInt(9.W))
//     // xorResult := 0.U
//     // for (i <- 1 until 10) {
//     //     xorResult := xorResult ^ io.buffer(i)
//     // }
// 
//     when (io.ready) {
//         when (io.nextdata_n  === 0.U)  {
//             r_ptr   :=  r_ptr   +   1.U
//             when (w_ptr  === r_ptr   +   1.U) {
//                 // io.ready   :=  0.U
//                 ready_wire := false.B
//             }
//         }
//     }
// 
//     when (sampling) {
//         when (count === 10.U) {
//             when (buffer(0) === 0.U && io.ps2_data && (buffer(9, 1).asUInt().xorR())) {
//                 fifo(w_ptr) :=  buffer(8,1)
//                 // fifo(0.U)   :=  buffer(8,1)
//                 w_ptr       :=  w_ptr   +   1.U
//                 io.ready    :=  true.B
//                 io.overflow :=  r_ptr   === (w_ptr  +   1.U)
//             }
//         }.otherwise {
//             // buffer(count)   :=  io.ps2_data
//             count           :=  count   +   1.U
//         }
//     }
// 
//     io.ready := ready_wire    
//     io.data :=  fifo(r_ptr)
//     // io.data :=  fifo(1.U)
//     // io.data :=  fifo<r_ptr>
// }


class ps2_keyboard extends Module {
  val io = IO(new Bundle {
    // val clk         = Input(Clock())
    // val clrn        = Input(Reset())
    val ps2_clk     = Input(Bool())
    val ps2_data    = Input(Bool())
    val nextdata_n  = Input(Bool())

    val data        = Output(UInt(8.W))
    val ready       = Output(Bool())
    val overflow    = Output(Bool())
  })

//   val buffer        = RegInit(0.U(10.W))
//   val buffer        = RegInit(VecInit(Seq.fill(0.U(8.W))))
//   val buffer    =   Reg(Vec(8))

  // buffer[10][1]
  // val buffer        = RegInit(VecInit(Seq.fill(10)(0.U(1.W))))
  val buffer        = Reg(Vec(10, UInt(1.W)))

  // fifo[8][8]
  // fifo[0][8] <= buffer[]
  val fifo          = Reg(Vec(8, UInt(8.W)))
  // val fifo          = RegInit(VecInit(Seq.fill(8)(0.U(8.W))))

  val bufferSlice   = buffer.slice(0, 8) // Take the first 8 elements of buffer

//   var buffer        = RegInit(0.U(10.W))
  val w_ptr         = RegInit(0.U(3.W))
  val r_ptr         = RegInit(0.U(3.W))
  val count         = RegInit(0.U(4.W))
  val ps2_clk_sync  = RegInit(0.U(3.W))
  val readyreg      = RegInit(false.B)
  val overflowreg   = RegInit(false.B)

  val sampling      = Wire(Bool())

  ps2_clk_sync      := Cat(ps2_clk_sync(1, 0), io.ps2_clk)
  sampling          := ps2_clk_sync(2) & ~ps2_clk_sync(1)

//   when (io.clrn === 0.U) {
//     count := 0.U
//     w_ptr := 0.U
//     r_ptr := 0.U
//     io.overflow := false.B
//     io.ready := false.B
//   } .otherwise {
    when (readyreg) {
      when (io.nextdata_n === false.B) {
        r_ptr := r_ptr + 1.U
        when (w_ptr === r_ptr + 1.U) {
          readyreg := false.B
        }
      }
    }

    // val xorResult = if (buffer.nonEmpty) buffer.slice(1, 10).reduce(_ ^ _) else 0.U

    when (sampling) {
      when (count === 10.U) {
        // when (buffer(0) === 0.U && io.ps2_data && (xorResult === 1.U)) {
        when (buffer(0) === 0.U && io.ps2_data) {
          // int the generated verilog, the result equals to zero?
          fifo(w_ptr) := Cat(buffer.slice(8, 0))
          // fifo(w_ptr) := 10.U
          // fifo(w_ptr) := buffer.slice(8, 1)
//           for(i <- 0 until 8) {
//             // fifo(w_ptr)(i)  :=  buffer(i)
//               val bufferValue = bufferSlice(i) // Extract the value from buffer
//   fifo(w_ptr)(i) := bufferValue    // Assign the value to fifo
// 
//           }

          w_ptr       := w_ptr + 1.U
          readyreg    := true.B
          overflowreg := r_ptr === (w_ptr + 1.U)
        }
        count := 0.U
      } .otherwise {

        buffer(count) := io.ps2_data
        // 2.buffer  :=  buffer  |   (io.ps2_data << count) 

        count := count + 1.U
      }
    }
//   }

    io.data     :=  fifo(r_ptr)
    io.ready    :=  readyreg
    io.overflow :=  overflowreg
}


object kb_main  extends App {
    val rela_path:  String  =   "lab7/kb/vsrc"
    emitVerilog(new ps2_keyboard(), Array("--target-dir", Path.path + rela_path))
}
