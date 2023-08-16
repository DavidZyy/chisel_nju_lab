package empty
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class Fir_test extends AnyFlatSpec with ChiselScalatestTester {
// Simple sanity check: a element with all zero coefficients should always produce zero
"Firtest" should "work" in {
// test(new My4ElementFir(0, 0, 0, 0)) { c =>
//     c.io.in.poke(0.U)
//     c.io.out.expect(0.U)
//     c.clock.step(1)
//     c.io.in.poke(4.U)
//     c.io.out.expect(0.U)
//     c.clock.step(1)
//     c.io.in.poke(5.U)
//     c.io.out.expect(0.U)
//     c.clock.step(1)
//     c.io.in.poke(2.U)
//     c.io.out.expect(0.U)
// }
// Simple 4-point moving average
// test(new My4ElementFir(1, 1, 1, 1)) { c =>
//     c.io.in.poke(1.U)
//     c.io.out.expect(1.U)  // 1, 0, 0, 0
//     c.clock.step(1)
//     c.io.in.poke(4.U)
//     c.io.out.expect(5.U)  // 4, 1, 0, 0
//     c.clock.step(1)
//     c.io.in.poke(3.U)
//     c.io.out.expect(8.U)  // 3, 4, 1, 0
//     c.clock.step(1)
//     c.io.in.poke(2.U)
//     c.io.out.expect(10.U)  // 2, 3, 4, 1
//     c.clock.step(1)
//     c.io.in.poke(7.U)
//     c.io.out.expect(16.U)  // 7, 2, 3, 4
//     c.clock.step(1)
//     c.io.in.poke(0.U)
//     c.io.out.expect(12.U)  // 0, 7, 2, 3
// }
// Nonsymmetric filter
test(new My4ElementFir(1, 2, 3, 4)) { c =>
    c.io.in.poke(1.U)
    c.io.out.expect(1.U)  // 1*1, 0*2, 0*3, 0*4
    c.clock.step(1)
    c.io.in.poke(4.U)
    c.io.out.expect(6.U)  // 4*1, 1*2, 0*3, 0*4
    c.clock.step(1)
    c.io.in.poke(3.U)
    c.io.out.expect(14.U)  // 3*1, 4*2, 1*3, 0*4
    c.clock.step(1)
    c.io.in.poke(2.U)
    c.io.out.expect(24.U)  // 2*1, 3*2, 4*3, 1*4
    c.clock.step(1)
    c.io.in.poke(7.U)
    c.io.out.expect(36.U)  // 7*1, 2*2, 3*3, 4*4
    c.clock.step(1)
    c.io.in.poke(0.U)
    c.io.out.expect(32.U)  // 0*1, 7*2, 2*3, 3*4
}
}
}
