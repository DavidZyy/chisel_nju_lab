package empty

import chisel3._
import chisel3.util._

class My4ElementFir(b0: Int, b1: Int, b2: Int, b3: Int) extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(8.W))
    val out = Output(UInt(8.W))
  })
    
  val reg1 = RegInit(0.U(8.W))
  val reg2 = RegInit(0.U(8.W))
  val reg3 = RegInit(0.U(8.W))
    
  reg1 := io.in
  reg2 := reg1
  reg3 := reg2

  io.out  :=  b0.U(8.W) * io.in + b1.U(8.W) * reg1 + b2.U(8.W) * reg2 + b3.U(8.W) * reg3
}


// import dspblocks._
// import freechips.rocketchip.amba.axi4._
// import freechips.rocketchip.amba.axi4stream._
// import freechips.rocketchip.config._
// import freechips.rocketchip.diplomacy._
// import freechips.rocketchip.regmapper._
// 
// //
// // Base class for all FIRBlocks.
// // This can be extended to make TileLink, AXI4, APB, AHB, etc. flavors of the FIR filter
// //
// abstract class FIRBlock[D, U, EO, EI, B <: Data](val nFilters: Int, val nTaps: Int)(implicit p: Parameters)
// // HasCSR means that the memory interface will be using the RegMapper API to define status and control registers
// extends DspBlock[D, U, EO, EI, B] with HasCSR {
//     // diplomatic node for the streaming interface
//     // identity node means the output and input are parameterized to be the same
//     val streamNode = AXI4StreamIdentityNode()
//     
//     // define the what hardware will be elaborated
//     lazy val module = new LazyModuleImp(this) {
//         // get streaming input and output wires from diplomatic node
//         val (in, _)  = streamNode.in(0)
//         val (out, _) = streamNode.out(0)
// 
//         require(in.params.n >= nFilters,
//                 s"""AXI-4 Stream port must be big enough for all 
//                    |the filters (need $nFilters,, only have ${in.params.n})""".stripMargin)
// 
//         // make registers to store taps
//         val taps = Reg(Vec(nFilters, Vec(nTaps, UInt(8.W))))
// 
//         // memory map the taps, plus the first address is a read-only field that says how many filter lanes there are
//         val mmap = Seq(
//             RegField.r(64, nFilters.U, RegFieldDesc("nFilters", "Number of filter lanes"))
//         ) ++ taps.flatMap(_.map(t => RegField(8, t, RegFieldDesc("tap", "Tap"))))
// 
//         // generate the hardware for the memory interface
//         // in this class, regmap is abstract (unimplemented). mixing in something like AXI4HasCSR or TLHasCSR
//         // will define regmap for the particular memory interface
//         regmap(mmap.zipWithIndex.map({case (r, i) => i * 8 -> Seq(r)}): _*)
// 
//         // make the FIR lanes and connect inputs and taps
//         val outs = for (i <- 0 until nFilters) yield {
//             val fir = Module(new MyManyDynamicElementVecFir(nTaps))
//             
//             fir.io.in := in.bits.data((i+1)*8, i*8)
//             fir.io.valid := in.valid && out.ready
//             fir.io.consts := taps(i)            
//             fir.io.out
//         }
// 
//         val output = if (outs.length == 1) {
//             outs.head
//         } else {
//             outs.reduce((x: UInt, y: UInt) => Cat(y, x))
//         }
// 
//         out.bits.data := output
//         in.ready  := out.ready
//         out.valid := in.valid
//     }
// }
// 
// // make AXI4 flavor of FIRBlock
// abstract class AXI4FIRBlock(nFilters: Int, nTaps: Int)(implicit p: Parameters) extends FIRBlock[AXI4MasterPortParameters, AXI4SlavePortParameters, AXI4EdgeParameters, AXI4EdgeParameters, AXI4Bundle](nFilters, nTaps) with AXI4DspBlock with AXI4HasCSR {
//     override val mem = Some(AXI4RegisterNode(
//         AddressSet(0x0, 0xffffL), beatBytes = 8
//     ))
// }

// running the code below will show what firrtl is generated
// note that LazyModules aren't really chisel modules- you need to call ".module" on them when invoking the chisel driver
// also note that AXI4StandaloneBlock is mixed in- if you forget it, you will get weird diplomacy errors because the memory
// interface expects a master and the streaming interface expects to be connected. AXI4StandaloneBlock will add top level IOs
// println(chisel3.Driver.emit(() => LazyModule(new AXI4FIRBlock(1, 8)(Parameters.empty) with AXI4StandaloneBlock).module))
