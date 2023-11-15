package rv32e.cache

import chisel3._
import chisel3.util._
import rv32e.config.Configs._
import rv32e.config.Cache_Configs._
import rv32e.config.Axi_Configs._
import rv32e.define.Dec_Info._
import rv32e.define.Inst._
import rv32e.define.CSR_Info._
import rv32e.utils.DiffCsr
import rv32e.bus._

// class D_Cache extends Module {
//     val from_LSU = IO(Flipped)
// }