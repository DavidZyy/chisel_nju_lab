// package learn.multiplier
// 
// import chisel3._
// import chiseltest._
// import chisel3.util._
// import org.scalatest.flatspec.AnyFlatSpec
// import learn.adder.Constants._
// import java.util.Random
// 
// class booth2Test extends AnyFlatSpec with ChiselScalatestTester {
//     val bitWidth = 64
//     "booth2" should "pass" in {
//         test(new booth2bit(bitWidth)) { dut =>
//             for(i <- 0 until 1000) {
//                 val A = BigInt(bitWidth, new Random) - (BigInt(1) << (bitWidth - 1))
//                 val B = BigInt(bitWidth, new Random) - (BigInt(1) << (bitWidth - 1))
//                 // val A = BigInt(-1) //-2 
//                 // val B = BigInt(-7)
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
//                 dut.clock.step(bitWidth/2)
// 
//                 dut.io.product.expect(product.S)
// 
//             }
//         }
//     }
// }
