// package learn
// 
// import chisel3._
// import chiseltest._
// import chisel3.util._
// import org.scalatest.flatspec.AnyFlatSpec
// import adder.SerialCarryAdder
// 
// trait ACA64TestFunc {
//     def testFn(dut: ACA64): Unit = {
//         dut.io.a.poke(4.U)
//         dut.io.b.poke(4.U)
//         dut.io.sum.expect(8.U)
//     }
// }
// 
// // trait decodeTestFunc {
// //     def testFn(dut: SimpleDecoder): Unit = {
// //         // Test all possible inputs and check the output
// //         for (i <- 0 until 8) {
// //             dut.input.poke(i.U)
// //             i match {
// //                 case 4 | 5 | 7 => dut.output.expect(1.U) // Defined outputs
// //                 case _ => dut.output.expect(1.U) // Undefined or default outputs
// //             }
// //         }
// //     }
// // }
// 
// trait SerialCarryAdderTestFunc {
//     def testFn(dut: SerialCarryAdder): Unit = {
//         dut.io.a.poke(10.U)
//         dut.io.b.poke(10.U)
//         dut.io.cin.poke(1.U)
//         dut.io.sum.expect(21.U)
//     }
// }
// 
// class SerialCarryAdderTest extends AnyFlatSpec with ChiselScalatestTester with SerialCarryAdderTestFunc {
//     "SerialCarryAdder" should "pass" in {
//         test(new SerialCarryAdder) { dut =>
//             testFn(dut)
//         }
//     }
// }
// 
// class ACA64Test extends AnyFlatSpec with ChiselScalatestTester with ACA64TestFunc {
//     "ACA64" should "pass" in {
//         test(new ACA64) { dut =>
//             testFn(dut)
//         }
//     }
// }
