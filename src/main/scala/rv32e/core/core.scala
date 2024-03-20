// package rv32e.core
// 
// import chisel3._
// import chisel3.util._
// import chisel3.util.BitPat
// import chisel3.util.experimental.decode._
// import chisel3.util.experimental.BoringUtils
// import chisel3.stage._
// 
// import _root_.circt.stage.ChiselStage
// import _root_.circt.stage.CIRCTTargetAnnotation
// import _root_.circt.stage.CIRCTTarget
// import _root_.circt.stage.FirtoolOption
// 
// import rv32e.bus._
// import rv32e.bus.simplebus._
// 
// import rv32e.core.backend._
// import rv32e.core.frontend._
// import rv32e.core.define.Mem._
// import rv32e.core.config._
// import rv32e.core.mem.cache._
// 
// import rv32e.device._
// 
// import rv32e.utils._
// 
// class top extends Module {
//     val io = IO(new Bundle{
//         val out = (new out_class)
//     })
// 
//     val IDU_i   =   Module(new IDU())
//     val ISU_i   =   Module(new ISU())
//     val EXU_i   =   Module(new EXU_pipeline()) 
//     val WBU_i   =   Module(new WBU())
// 
//     /* ifu connect to cache */
//     val IFU_i   =   Module(new IFU_pipeline())
//     val ram_i   =   Module(new AXI4RAM())
// 
//     
//     val icache  =   Module(new Cache(DATA_WIDTH, "icache"))
//     IFU_i.to_mem  <> icache.io.in
//     IFU_i.to_IDU_PC := icache.io.stage2Addr
//     icache.io.mem.toAXI4() <> ram_i.axi
//     icache.io.flush := WBU_i.to_IFU.bits.redirect.valid
// 
//     val ram_i2 =   Module(new AXI4RAM())
// 
//     val addrSpace = List(
//         (pmemBase, pmemSize),
//         (RTC_ADDR, 8L),
//         (RTC_ADDR+8L, mmioSize),
//         // (mmioBase, mmioSize),
//     )
//     val memXbar = Module(new SimpleBusCrossBar1toN(addrSpace))
// 
//     val dcache  =   Module(new Cache(DATA_WIDTH, "dcache"))
//     val mmio    =   Module(new MMIO())
//     val clint   =   Module(new AXI4CLINT())
// 
//     EXU_i.lsu_to_mem  <> memXbar.io.in
//     memXbar.io.flush  := WBU_i.to_IFU.bits.redirect.valid
//     memXbar.io.out(0) <> dcache.io.in
//     memXbar.io.out(1).toAXI4Lite() <> clint.io.in
//     // memXbar.io.out(2) <> mmio.from_lsu
//     memXbar.io.out(2) <> mmio.io.in
//     mmio.io.flush := WBU_i.to_IFU.bits.redirect.valid
// 
//     dcache.io.mem.toAXI4() <> ram_i2.axi
//     dcache.io.flush := WBU_i.to_IFU.bits.redirect.valid
// 
// 
//     WBU_i.to_IFU <> IFU_i.from_WBU
//     PipelineConnect(IFU_i.to_IDU, IDU_i.from_IFU, IDU_i.to_ISU.fire, WBU_i.to_IFU.bits.redirect.valid)// && IFU_i.to_IDU.fire)
//     PipelineConnect(IDU_i.to_ISU, ISU_i.from_IDU, ISU_i.to_EXU.fire, WBU_i.to_IFU.bits.redirect.valid)// && IDU_i.to_ISU.fire)
//     PipelineConnect(ISU_i.to_EXU, EXU_i.from_ISU, EXU_i.to_WBU.fire, WBU_i.to_IFU.bits.redirect.valid)// && ISU_i.to_EXU.fire)
//     PipelineConnect(EXU_i.to_WBU, WBU_i.from_EXU, WBU_i.wb, WBU_i.to_IFU.bits.redirect.valid)// && EXU_i.to_WBU.fire)
//     EXU_i.to_ISU <> ISU_i.from_EXU
//     EXU_i.npc    := ISU_i.to_EXU.bits.pc
//     WBU_i.to_ISU <> ISU_i.from_WBU
//     ISU_i.flush  := WBU_i.to_IFU.bits.redirect.valid
// 
//     ////////////// for perf ///////////////
//     if (EnablePerfCnt) {
//         val PerfCnt_i = Module(new perfCnt())
//         BoringUtils.addSource(WireInit(ISU_i.to_EXU.fire && ISU_i.to_EXU.bits.isLSU), perfPrefix+"nrLSU")
//         BoringUtils.addSource(WireInit(ISU_i.to_EXU.fire && ISU_i.to_EXU.bits.isBRU), perfPrefix+"nrBRU")
//         BoringUtils.addSource(WireInit(ISU_i.to_EXU.fire && ISU_i.to_EXU.bits.isCSR), perfPrefix+"nrCSR")
//         BoringUtils.addSource(WireInit(ISU_i.to_EXU.fire && ISU_i.to_EXU.bits.isALU), perfPrefix+"nrALU")
//         BoringUtils.addSource(WireInit(ISU_i.to_EXU.fire && ISU_i.to_EXU.bits.isMDU), perfPrefix+"nrMDU")
//     }
//     
//     ////////////// for output ///////////////
//     when(WBU_i.from_EXU.valid) {
//         io.out.nextExecPC := WBU_i.from_EXU.bits.pc
//     } .elsewhen(EXU_i.from_ISU.valid) {
//         io.out.nextExecPC := EXU_i.from_ISU.bits.pc
//     } .elsewhen(ISU_i.from_IDU.valid) {
//         io.out.nextExecPC := ISU_i.from_IDU.bits.pc
//     } .elsewhen(IDU_i.from_IFU.valid) {
//         io.out.nextExecPC := IDU_i.from_IFU.bits.pc
//     } .elsewhen(IFU_i.to_IDU.valid) {
//         io.out.nextExecPC := IFU_i.to_IDU.bits.pc
//     } .otherwise {
//         io.out.nextExecPC := IFU_i.fetch_PC
//     }
// 
//     io.out.ifu_fetchPc := IFU_i.fetch_PC
//     io.out.ifu.pc   := IFU_i.to_IDU.bits.pc       // actually icache pc
//     io.out.ifu.inst := IFU_i.to_IDU.bits.inst
//     io.out.idu.pc   := IDU_i.from_IFU.bits.pc
//     io.out.idu.inst := IDU_i.from_IFU.bits.inst
//     io.out.isu.pc   := ISU_i.from_IDU.bits.pc
//     io.out.isu.inst := ISU_i.from_IDU.bits.inst
//     io.out.exu.pc   := EXU_i.from_ISU.bits.pc
//     io.out.exu.inst := EXU_i.from_ISU.bits.inst
//     io.out.wbu.pc   := WBU_i.from_EXU.bits.pc
//     io.out.wbu.inst := WBU_i.from_EXU.bits.inst
// 
//     io.out.wb       := WBU_i.wb
//     io.out.difftest <> EXU_i.difftest
//     io.out.is_mmio  := WBU_i.is_mmio
// }
// 
// object top_main extends App {
//     def t = new top()
//     val generator = Seq(
//         chisel3.stage.ChiselGeneratorAnnotation(() => t),
//         CIRCTTargetAnnotation(CIRCTTarget.Verilog)
//     )
//     val firtoolOptions = Seq(
//         FirtoolOption("--disable-all-randomization"),
//         FirtoolOption("--lowering-options=disallowLocalVariables, locationInfoStyle=none"),
//         // FirtoolOption("--lowering-options=locationInfoStyle=none")
//     )
//     (new ChiselStage).execute(args, generator ++ firtoolOptions)
// }