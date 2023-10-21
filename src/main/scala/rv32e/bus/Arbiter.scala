// package rv32e.bus
// 
// import chisel3._
// import chisel3.util._
// import chisel3.util.BitPat
// import chisel3.util.experimental.decode._
// import rv32e.config.Configs._
// import rv32e.define.Dec_Info._
// import rv32e.define.Inst._
// import rv32e.utils.LFSR
// 
// 
// class Arbiter extends Module {
//     val from_master1 = IO(new AXILiteIO_slave)
//     val from_master2 = IO(new AXILiteIO_slave)
//     val to_slave     = IO(new AXILiteIO_master)
// 
//     val s_idle :: Nil = Enum(1)
//     val state = RegInit(s_idle)
// 
//     // axi.ar.ready    :=  
//     switch (state) {
// 
//     }
// }
