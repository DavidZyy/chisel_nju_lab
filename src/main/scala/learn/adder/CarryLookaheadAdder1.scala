// /**
//   * Intra-block parallel, inter-block serial adder
//   */
// package learn.adder
// 
// import chisel3._
// import chisel3.util._
// import Constants._
// 
// class CarryLookaheadAdderBlock1 extends Module {
//   val io = IO(new Bundle {
//     val p    = Input(UInt(BLK_WDT.W))
//     val g    = Input(UInt(BLK_WDT.W))
//     val cin  = Input(UInt(1.W)) //c0
//     val c    = Output(UInt(BLK_WDT.W))
//     val cout = Output(UInt(1.W)) //c4
//   })
// 
//   val c = Wire(Vec(BLK_WDT, UInt(1.W)))
//   val p = io.p
//   val g = io.g
// 
//   c(0) := io.cin
//   c(1) := g(0) | (p(0) & c(0))
//   c(2) := g(1) | (p(1) & g(0)) | (p(1) & p(0) & c(0))
//   c(3) := g(2) | (p(2) & g(1)) | (p(2) & p(1) & g(0)) | (p(2) & p(1) & p(0) & c(0))
// 
//   io.cout := g(3) | (p(3) & g(2)) | (p(3) & p(2) & g(1)) | (p(3) & p(2) & p(1) & g(0)) | (p(3) & p(2) & p(1) & p(0) & c(0))
//   io.c := c.asUInt()
// }
// 
// class adder_v1 extends Module {
//   val blk_num = ADR_WDT/BLK_WDT
//   val io = IO(new Bundle {
//     val A    = Input(UInt(ADR_WDT.W))
//     val B    = Input(UInt(ADR_WDT.W))
//     val Cin  = Input(UInt(1.W))
//     val Sum  = Output(UInt(ADR_WDT.W))
//     val Cout = Output(UInt(1.W))
//   })
// 
//   // Instantiate 16 blocks (4 bits each)
//   val blocks = Seq.fill(blk_num)(Module(new CarryLookaheadAdderBlock1()))
// 
//   val p = io.A | io.B
//   val g = io.A & io.B
//   val c = Wire(Vec(blk_num, UInt(BLK_WDT.W)))
// 
//   // Connect inputs to the first block
//   blocks(0).io.p := p(BLK_WDT-1, 0)
//   blocks(0).io.g := g(BLK_WDT-1, 0)
//   blocks(0).io.cin := io.Cin
//   c(0) := blocks(0).io.c
// 
//   // Connect blocks in a chain
//   for (i <- 1 until blk_num) {
//     blocks(i).io.p   := p((i+1)*BLK_WDT-1, i*BLK_WDT)
//     blocks(i).io.g   := g((i+1)*BLK_WDT-1, i*BLK_WDT)
//     blocks(i).io.cin := blocks(i - 1).io.cout
//     c(i) := blocks(i).io.c
//   }
// 
//   // Create an array of 64 full adders
//   val fullAdders = VecInit(Seq.fill(ADR_WDT)(Module(new FullAdder()).io))
// 
//   val cc = c.asUInt()
//   for (i <- 0 until ADR_WDT) {
//     fullAdders(i).a := io.A(i)
//     fullAdders(i).b := io.B(i)
//     fullAdders(i).cin := cc(i)
//   }
// 
//   io.Sum := Reverse(Cat(fullAdders.map(_.sum))).asUInt()
//   io.Cout := blocks(blk_num-1).io.cout
// }
// 
// object adder_v1 extends App {
//   emitVerilog(new adder_v1(), Array("--target-dir", "generated"))
// }
