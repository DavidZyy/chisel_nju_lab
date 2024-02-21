// package learn
// 
// import chisel3._
// import chisel3.util._
// 
// object LookupTree {
//   def apply[T <: Data](key: UInt, mapping: Iterable[(UInt, T)]): T =
//     Mux1H(mapping.map(p => (p._1 === key, p._2)))
// }
// 
// object LookupTreeDefault {
//   def apply[T <: Data](key: UInt, default: T, mapping: Iterable[(UInt, T)]): T =
//     MuxLookup(key, default)(mapping.toSeq)
// }
// 
// object MaskData {
//   def apply(oldData: UInt, newData: UInt, fullmask: UInt) = {
//     require(oldData.getWidth == newData.getWidth)
//     require(oldData.getWidth == fullmask.getWidth)
//     (newData & fullmask) | (oldData & ~fullmask)
//   }
// }
// 
// // test of regmap scala.
// object MaskedRegMap {
//   def Unwritable = null
//   def NoSideEffect: UInt => UInt = (x=>x)
//   def WritableMask = Fill( 32, true.B)
//   def UnwritableMask = 0.U(32.W)
//   def apply(addr: Int, reg: UInt, wmask: UInt = WritableMask, wfn: UInt => UInt = (x => x), rmask: UInt = WritableMask) = (addr, (reg, wmask, wfn, rmask))
//   def generate(mapping: Map[Int, (UInt, UInt, UInt => UInt, UInt)], raddr: UInt, rdata: UInt, waddr: UInt, wen: Bool, wdata: UInt):Unit = {
//     val chiselMapping = mapping.map { case (a, (r, wm, w, rm)) => (a.U, r, wm, w, rm) }
//     rdata := LookupTree(raddr, chiselMapping.map { case (a, r, wm, w, rm) => (a, r & rm) })
//     chiselMapping.map { case (a, r, wm, w, rm) =>
//       if (w != null && wm != UnwritableMask) when (wen && waddr === a) { r := w(MaskData(r, wdata, wm)) }
//     }
//   }
//   def isIllegalAddr(mapping: Map[Int, (UInt, UInt, UInt => UInt, UInt)], addr: UInt):Bool = {
//     val illegalAddr = Wire(Bool())
//     val chiselMapping = mapping.map { case (a, (r, wm, w, rm)) => (a.U, r, wm, w, rm) }
//     illegalAddr := LookupTreeDefault(addr, true.B, chiselMapping.map { case (a, r, wm, w, rm) => (a, false.B) })
//     illegalAddr
//   }
//   def generate(mapping: Map[Int, (UInt, UInt, UInt => UInt, UInt)], addr: UInt, rdata: UInt,
//     wen: Bool, wdata: UInt):Unit = generate(mapping, addr, rdata, addr, wen, wdata)
// }
// 
// 
// // object MaskData {
// //   def apply(oldData: UInt, newData: UInt, mask: UInt) = {
// //     (newData & mask) | (oldData & ~mask)
// //   }
// // }
// 
// object CSRRegMap {
//   def DefaultMask = Fill(64, 1.U)
// 
//   // type is splited by "," !
//   def apply(addr: UInt, reg: UInt, wmask: UInt = DefaultMask, wfn: UInt => UInt = (x => x), rmask: UInt = DefaultMask) = (addr, (reg, wmask, wfn, rmask))
// 
//   def access(mapping: Map[UInt, (UInt, UInt, UInt => UInt, UInt)], addr: UInt, rdata: UInt, wdata: UInt, wen: Bool): Unit = {
//     mapping.map {
//       case (a, (r, wm, wfn, rm)) => {
//         when (addr === a) {
//           rdata := r & rm
//         }
//         if (wfn != null) {
//           when (addr === a && wen) {
//             r := wfn(MaskData(r, wdata, wm))
//           }
//         }
//       }
//     }
//   }
// }
// 
// class CSR extends Module {
//   // val io = IO(new Bundle{
//   //   val out_mcycle   = Output(UInt(64.W))
//   //   val out_mepc     = Output(UInt(64.W))
//   //   val out_mcause   = Output(UInt(64.W))
//   //   val out_mstatus  = Output(UInt(64.W))
//   //   val out_mtvec    = Output(UInt(64.W))
//   //   val out_mip      = Output(UInt(64.W))
//   //   val out_mie      = Output(UInt(64.W))
//   //   val out_mscratch = Output(UInt(64.W))
//   // }) 
//     val io = IO(new Bundle() {
//     val addr = Input(UInt(12.W))
//     val wen = Input(Bool())
//     val rdata = Output(UInt(64.W))
//     val wdata = Input(UInt(64.W))
// 
//   })
//     val mcycle   = RegInit(0.U(64.W))
//     val mepc     = RegInit(0.U(64.W))
//     val mcause   = RegInit(0.U(64.W))
//     val mstatus  = RegInit("ha00001800".U(64.W))
//     val mtvec    = RegInit(0.U(64.W))
//     val mip      = RegInit(0.U(64.W))
//     val mie      = RegInit(0.U(64.W))
//     val mscratch = RegInit(0.U(64.W))
// 
//     io.rdata := 0.U
// 
// //     io.out_mcycle   := mcycle   
// //     io.out_mepc     := mepc     
// //     io.out_mcause   := mcause   
// //     io.out_mstatus  := mstatus  
// //     io.out_mtvec    := mtvec    
// //     io.out_mip      := mip      
// //     io.out_mie      := mie      
// //     io.out_mscratch := mscratch 
// // 
//     val CSR_WIDTH = 12
//     val mcycle_addr = "hb00".U(12.W)
//     val mepc_addr = "h341".U(12.W)
//     val mcause_addr = "h342".U(12.W)
//     val mstatus_addr = "h300".U(12.W)
//     val mtvec_addr = "h305".U(12.W)
//     val mip_addr = "h344".U(12.W)
//     val mie_addr = "h304".U(12.W)
//     val mscratch_addr = "h340".U(12.W)
// 
//    def mstatusSideEffect(mstatus: UInt): UInt = {
//     val mstatus_new = Cat((mstatus(16, 15) === "b11".U) || (mstatus(14, 13) === "b11".U), mstatus(62, 0))
//     mstatus_new
//   }
// 
//     val csr_map = Map(
//     CSRRegMap(mcycle_addr  , mcycle                                            ),
//     CSRRegMap(mepc_addr    , mepc    , "hfffffffffffffffc".U                   ),
//     CSRRegMap(mcause_addr  , mcause                                            ),
//     CSRRegMap(mstatus_addr , mstatus , "hffffffffffffffff".U, mstatusSideEffect),
//     CSRRegMap(mtvec_addr   , mtvec                                             ),
//     CSRRegMap(mip_addr     , mip     , "h0000000000000000".U                   ),
//     CSRRegMap(mie_addr     , mie                                               ),
//     CSRRegMap(mscratch_addr, mscratch                                          )
//   )
// 
//   CSRRegMap.access(csr_map, io.addr, io.rdata, io.wdata, io.wen)
// 
// 
// }
// 
// object csr_main extends App {
//     emitVerilog(new CSR(), Array("--target-dir", "generated"))
// }
