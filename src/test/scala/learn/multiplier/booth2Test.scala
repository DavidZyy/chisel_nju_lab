package learn.multiplier

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import learn.adder.Constants._
import java.util.Random

class booth2Test extends AnyFlatSpec with ChiselScalatestTester {
    val bitWidth = 8
    "booth2" should "pass" in {
        test(new booth2bit(bitWidth)) { dut =>
            for(i <- 0 until 10) {
                val A = BigInt(bitWidth, new Random) - (BigInt(1) << (bitWidth - 1))
                val B = BigInt(bitWidth, new Random) - (BigInt(1) << (bitWidth - 1))
                // val A = BigInt(-1) //-2 
                // val B = BigInt(-7)
                val product = A*B // should be the complete of -6
                // val product = BigInt(-6)

                println("A: " + A.toString(16) + " " + A)
                println("B: " + B.toString(16) + " " + B)
                println("product: " + product.toString(16) + " " + product)
                println() // '\n'



                dut.io.operandA.poke(A.S)
                dut.io.operandB.poke(B.S)

                dut.reset.poke(true.B) // Assert reset
                dut.clock.step(1)
                dut.reset.poke(false.B) // Deassert reset

                dut.clock.step(bitWidth/2)

                dut.io.product.expect(product.S)

            }
        }
    }
}


class booth2_1cTest extends AnyFlatSpec with ChiselScalatestTester {
    val bitWidth = 4
    "booth2" should "pass" in {
        test(new booth2_1c(bitWidth)) { dut =>
            for(i <- 0 until 100) {
                val A = BigInt(bitWidth, new Random) - (BigInt(1) << (bitWidth - 1))
                val B = BigInt(bitWidth, new Random) - (BigInt(1) << (bitWidth - 1))
                // val A = BigInt(-1) //-2 
                // val B = BigInt(-7)
                val product = A*B // should be the complete of -6
                // val product = BigInt(-6)

                println("A: " + A.toString(2) + " " + A)
                println("B: " + B.toString(2) + " " + B)
                println("product: " + product.toString(2) + " " + product)
                println() // '\n'

                dut.io.operandA.poke(A.S)
                dut.io.operandB.poke(B.S)

                dut.io.product.expect(product.S)
            }
        }
    }
}

class booth2_1c_unsignedTest extends AnyFlatSpec with ChiselScalatestTester {
    val bitWidth = 4
    "booth2" should "pass" in {
        test(new booth2_1c_unsigned(bitWidth)) { dut =>
            for(i <- 0 until 1000) {
                val A = BigInt(bitWidth, new Random)
                val B = BigInt(bitWidth, new Random)
                // val A = BigInt(-1) //-2 
                // val B = BigInt(-7)
                val product = A*B // should be the complete of -6
                // val product = BigInt(-6)

                println("A: " + A.toString(16) + " " + A)
                println("B: " + B.toString(16) + " " + B)
                println("product: " + product.toString(16) + " " + product)
                println() // '\n'

                dut.io.operandA.poke(A.U)
                dut.io.operandB.poke(B.U)

                dut.io.product.expect(product.U)
            }
        }
    }
}

class booth2Testx extends AnyFlatSpec with ChiselScalatestTester {
    val bitWidth = 8
    "booth2" should "pass" in {
        test(new booth2(bitWidth)) { dut =>
            for(i <- 0 until 100) {
                val mulSigned = BigInt(1, new Random)
                val A: BigInt =
                if (mulSigned == BigInt(0)) {
                  BigInt(bitWidth, new Random)
                } else {
                  BigInt(bitWidth, new Random) - (BigInt(1) << (bitWidth - 1))
                }

                val B: BigInt =
                if (mulSigned == BigInt(0)) {
                  BigInt(bitWidth, new Random)
                } else {
                  BigInt(bitWidth, new Random) - (BigInt(1) << (bitWidth - 1))
                }

                // val mulSigned = BigInt(1)
                // val A = BigInt(5)
                // val B = BigInt(-7)

                // val mulSigned = BigInt(0)
                // val A = BigInt(5)
                // val B = BigInt(9)

                val product = A*B

                val complementA: BigInt = 
                if(A >= BigInt(0)) { A }
                else { A + (BigInt(1) << bitWidth) }

                val complementB: BigInt = 
                if(B >= BigInt(0)) { B }
                else { B + (BigInt(1) << bitWidth) }

                val complementProduct: BigInt = 
                if(product >= BigInt(0)) {product}
                else (product + (BigInt(1) << 2*bitWidth))

                println("mulSigned: " + " " + mulSigned)
                println("A: " + A.toString(16) + " " + A)
                println("B: " + B.toString(16) + " " + B)
                println("product: " + product.toString(16) + " " + product)
                println() // '\n'

                dut.io.mulSigned.poke(mulSigned.B)
                dut.io.operandA.poke(complementA.U)
                dut.io.operandB.poke(complementB.U)

                dut.io.product.expect(complementProduct.U)
            }
        }
    }
}


class myMultipierTest extends AnyFlatSpec with ChiselScalatestTester {
    val bitWidth = 64
    "booth2" should "pass" in {
        test(new myMultipier(bitWidth)) { dut =>
            for(i <- 0 until 10) {
                val mulSigned = BigInt(1, new Random)
                val A: BigInt =
                if (mulSigned == BigInt(0)) {
                  BigInt(bitWidth, new Random)
                } else {
                  BigInt(bitWidth, new Random) - (BigInt(1) << (bitWidth - 1))
                }

                val B: BigInt =
                if (mulSigned == BigInt(0)) {
                  BigInt(bitWidth, new Random)
                } else {
                  BigInt(bitWidth, new Random) - (BigInt(1) << (bitWidth - 1))
                }

                // val mulSigned = BigInt(1)
                // val A = BigInt(5)
                // val B = BigInt(-7)

                // val mulSigned = BigInt(0)
                // val A = BigInt(5)
                // val B = BigInt(9)

                val product = A*B

                val complementA: BigInt = 
                if(A >= BigInt(0)) { A }
                else { A + (BigInt(1) << bitWidth) }

                val complementB: BigInt = 
                if(B >= BigInt(0)) { B }
                else { B + (BigInt(1) << bitWidth) }

                val complementProduct: BigInt = 
                if(product >= BigInt(0)) {product}
                else (product + (BigInt(1) << 2*bitWidth))

                println("mulSigned: " + " " + mulSigned)
                println("A: " + A.toString(16) + " " + A)
                println("B: " + B.toString(16) + " " + B)
                println("product: " + product.toString(16) + " " + product)
                println("complementA: " + complementA.toString(16) + " " + complementA)
                println("complementB: " + complementB.toString(16) + " " + complementB)
                println("complementProduct: " + complementProduct.toString(16) + " " + complementProduct)
                println() // '\n'

                dut.io.mulSigned.poke(mulSigned.B)
                dut.io.operandA.poke(complementA.U)
                dut.io.operandB.poke(complementB.U)

                // dut.io.PPGprodeuct.expect(complementProduct.U)
                dut.io.product.expect(complementProduct.U)
            }
        }
    }
}