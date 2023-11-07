// package rv32e.fu
// 
// import chisel3._
// import chisel3.util._
// import rv32e.config.Configs._
// import rv32e.define.Dec_Info._
// import rv32e.define.Inst._
// import rv32e.define.CSR_Info._
// import rv32e.utils.DiffCsr
// 
// class csr_in_class extends Bundle {
//   val op      = Input(UInt(CSROP_WIDTH.W))
//   val cur_pc  = Input(UInt(ADDR_WIDTH.W))
//   // the immediate field of I-type inst.
//   val addr    = Input(UInt(DATA_WIDTH.W)) // csr id
//   val wdata   = Input(UInt(DATA_WIDTH.W))
// }
// 
// class csr_out_class extends Bundle {
//   val csr_br   = Output(Bool()) // if jmp
//   val csr_addr = Output(UInt(ADDR_WIDTH.W)) // jmp addr
//   val rdata    = Output(UInt(DATA_WIDTH.W)) // read csr
//   val difftest = new DiffCsr
// }
// 
// object MaskData {
//   def apply(oldData: UInt, newData: UInt, mask: UInt) = {
//     (newData & mask) | (oldData & ~mask)
//   }
// }
// 
// object LookupTree {
//   def apply[T <: Data](key: UInt, mapping: Iterable[(UInt, T)]): T =
//     Mux1H(mapping.map(p => (p._1 === key, p._2)))
// }
// 
// object MaskedRegMap {
//     def DefaultMask = Fill(64, 1.U)
// 
//     def apply(addr: UInt, reg: UInt, wmask: UInt = DefaultMask, 
//               wfn: UInt => UInt = (x => x), rmask: UInt = DefaultMask) 
//               = (addr, (reg, wmask, wfn, rmask))
// 
//     def generate(mapping: Map[UInt, (UInt, UInt, UInt => UInt, UInt)], addr: UInt, 
//                 rdata: UInt, wdata: UInt, wen: Bool): Unit = {
//       val chiselMapping = mapping.map { case (a, (r, wm, w, rm)) => (a, r, wm, w, rm) }
//       rdata := LookupTree(addr, chiselMapping.map { case (a, r, wm, w, rm) => (a, r & rm) })
//       chiselMapping.map { case (a, r, wm, w, rm) =>
//         if (w != null) when (wen && addr === a) { r := w(MaskData(r, wdata, wm)) }
//       }
//     }
// }
// 
// class Csr extends Module {
//   val io = IO(new Bundle {
//     val in  = (new csr_in_class)
//     val out = (new csr_out_class)
//   })
// 
//   val reg_mepc    = RegInit(0.U(DATA_WIDTH.W))
//   val reg_mcause  = RegInit(0.U(DATA_WIDTH.W))
//   val reg_mstatus = RegInit(0.U(DATA_WIDTH.W))
//   val reg_mtvec   = RegInit(0.U(DATA_WIDTH.W))
// 
//   val rdata       = WireInit(0.U(DATA_WIDTH.W))
// 
//   io.out.csr_br := MuxLookup(io.in.op, false.B, Array(
//     ("b" + csr_ecall ).U  ->   true.B,
//     ("b" + csr_mret  ).U  ->   true.B,
//   ))
// 
//   io.out.csr_addr :=  MuxLookup(io.in.op, 0.U, Array(
//     ("b" + csr_ecall ).U  ->   reg_mtvec,
//     ("b" + csr_mret  ).U  ->   reg_mepc,
//   ))
// 
// 
//   io.out.rdata  :=  rdata
//   // io.out.r_csr   :=  MuxLookup(io.in.csr_id, 0.U, Array(
//   //   mtvec_id    -> reg_mtvec,
//   //   mepc_id     -> reg_mepc,
//   //   mcause_id   -> reg_mcause,
//   //   mstatus_id  -> reg_mstatus,
//   // ))
// 
//   io.out.difftest.mcause  := reg_mcause
//   io.out.difftest.mepc    := reg_mepc
//   io.out.difftest.mstatus := reg_mstatus
//   io.out.difftest.mtvec   := reg_mtvec
// }
