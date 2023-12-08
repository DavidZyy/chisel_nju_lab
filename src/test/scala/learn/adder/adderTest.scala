// package learn.adder
// 
// import chisel3._
// import chiseltest._
// import chisel3.util._
// import org.scalatest.flatspec.AnyFlatSpec
// import learn.adder.Constants._
// import java.util.Random
// // import random
// 
// 
// // class adder_v1Test extends AnyFlatSpec with ChiselScalatestTester {
// //   "adder_v1" should "correctly add two numbers" in {
// //     test(new adder_v1) { dut =>
// class adder_v2Test extends AnyFlatSpec with ChiselScalatestTester {
//   "adder_v2" should "correctly add two numbers" in {
//     test(new adder_v2) { dut =>
//         for(i <- 0 until 100) {
//             val A = BigInt(ADR_WDT, new Random)
//             val B = BigInt(ADR_WDT, new Random)
//             val Cin = BigInt(1, new Random)
//    
// 
//             // Set the inputs
//             dut.io.A.poke(A.U)
//             dut.io.B.poke(B.U)
//             dut.io.Cin.poke((Cin.U))
// 
//             // Calculate the expected sum
//             val Sum  = (A + B + Cin) & ((BigInt(1) << ADR_WDT) - 1)
//             val Cout = ((A+B) > Sum).B
// 
//             // Check if the output matches the expected sum
//             dut.io.Sum.expect(Sum.U)
//             dut.io.Cout.expect(Cout.asUInt)
// 
//             println(A)
//             println(B)
//             println(Cout.asUInt)
//         }
//     }
//   }
// }
