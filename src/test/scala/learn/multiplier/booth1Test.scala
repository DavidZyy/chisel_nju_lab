// package learn.multiplier
// 
// import chisel3._
// import chiseltest._
// import chisel3.util._
// import org.scalatest.flatspec.AnyFlatSpec
// import learn.adder.Constants._
// import java.util.Random
// 
// class booth1Test extends AnyFlatSpec with ChiselScalatestTester {
//     val bitWidth = 4
//     "booth1" should "pass" in {
//         test(new booth1bit(bitWidth)) { dut =>
//             // for(i <- 0 until 100) {
//                 // val A = BigInt(bitWidth, new Random) - (BigInt(1) << (bitWidth - 1))
//                 // val B = BigInt(bitWidth, new Random) - (BigInt(1) << (bitWidth - 1))
//                 val A = BigInt(-2) //-2 
//                 val B = BigInt(3)
//                 val product = A*B // should be the complete of -6
//                 // val product = BigInt(-6)
// 
//                 println("A:", A)
//                 println("B:", B)
//                 println("product: ", product)
// 
//                 dut.io.operandA.poke(A.S)
//                 dut.io.operandB.poke(B.S)
// 
//                 dut.reset.poke(true.B) // Assert reset
//                 dut.clock.step(1)
//                 dut.reset.poke(false.B) // Deassert reset
// 
//                 dut.clock.step(bitWidth)
// 
//                 dut.io.product.expect(product.S)
// 
//             // }
//         }
//     }
// }
