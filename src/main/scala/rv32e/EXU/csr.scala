package rv32e.EXU

import chisel3._
import chisel3.util._
import rv32e.config.Configs._
import rv32e.config.Dec_Info._
import rv32e.config.Inst._
import rv32e.config.CSR_INFO._

class csr_in_class extends Bundle {
  val csr_op  = Input(UInt(CSROP_WIDTH.W))
  val cur_pc  = Input(UInt(ADDR_WIDTH.W))
  // the immediate field of I-type inst.
  val csr_id  = Input(UInt(DATA_WIDTH.W))
  val wdata   = Input(UInt(DATA_WIDTH.W))
}

class Regs extends Bundle {
  val mcause  = Output(UInt(DATA_WIDTH.W))
  val mepc    = Output(UInt(DATA_WIDTH.W))
  val mstatus = Output(UInt(DATA_WIDTH.W))
  val mtvec   = Output(UInt(DATA_WIDTH.W))
}

class csr_out_class extends Bundle {
  val ctrl_csr = Output(Bool()) // if jmp
  val csr_pc   = Output(UInt(ADDR_WIDTH.W)) // jmp addr
  val r_csr    = Output(UInt(DATA_WIDTH.W))
  val difftest = new Regs
}

class CSR extends Module {
  val io = IO(new Bundle {
    val in  = (new csr_in_class)
    val out = (new csr_out_class)
  })

  // val clk = Clock()

  val reg_mepc    = RegInit(0.U(DATA_WIDTH.W))
  val reg_mcause  = RegInit(0.U(DATA_WIDTH.W))
  val reg_mstatus = RegInit(0.U(DATA_WIDTH.W))
  val reg_mtvec   = RegInit(0.U(DATA_WIDTH.W))

  // val r_csr       = WireInit(0.U(DATA_WIDTH.W))
  val reg_mcause_csrrw   = WireInit(0.U(DATA_WIDTH.W))
  val reg_mcause_csrrs   = WireInit(0.U(DATA_WIDTH.W))
  val reg_mepc_csrrw     = WireInit(0.U(DATA_WIDTH.W))
  val reg_mepc_csrrs     = WireInit(0.U(DATA_WIDTH.W))
  val reg_mstatus_csrrw  = WireInit(0.U(DATA_WIDTH.W))
  val reg_mstatus_csrrs  = WireInit(0.U(DATA_WIDTH.W))
  val reg_mtvec_csrrw    = WireInit(0.U(DATA_WIDTH.W))
  val reg_mtvec_csrrs    = WireInit(0.U(DATA_WIDTH.W))

  reg_mcause_csrrw := MuxLookup(io.in.csr_id, reg_mcause, Array(
    mcause_id  -> io.in.wdata,
  ))
  reg_mcause_csrrs := MuxLookup(io.in.csr_id, reg_mcause, Array(
    mcause_id  -> (io.in.wdata | reg_mcause),
  ))
  reg_mcause := MuxLookup(io.in.csr_op, reg_mcause, Array(
    ("b" + csr_ecall ).U  ->   0xb.U,
    ("b" + csr_csrrw ).U  ->   reg_mcause_csrrw,
    ("b" + csr_csrrs ).U  ->   reg_mcause_csrrs,
  ))

  reg_mepc_csrrw := MuxLookup(io.in.csr_id, reg_mepc, Array(
    mepc_id  -> io.in.wdata,
  ))
  reg_mepc_csrrs := MuxLookup(io.in.csr_id, reg_mepc, Array(
    mepc_id  -> (io.in.wdata | reg_mepc),
  ))
  reg_mepc := MuxLookup(io.in.csr_op, reg_mepc, Array(
    ("b" + csr_ecall ).U  ->   io.in.cur_pc,
    ("b" + csr_csrrw ).U  ->   reg_mepc_csrrw,
    ("b" + csr_csrrs ).U  ->   reg_mepc_csrrs,
  ))

  reg_mstatus_csrrw := MuxLookup(io.in.csr_id, reg_mstatus, Array(
    mstatus_id  -> io.in.wdata,
  ))
  reg_mstatus_csrrs := MuxLookup(io.in.csr_id, reg_mstatus, Array(
    mstatus_id  -> (io.in.wdata | reg_mstatus),
  ))
  reg_mstatus := MuxLookup(io.in.csr_op, reg_mstatus, Array(
    ("b" + csr_ecall ).U  ->   Cat( reg_mstatus(sd_MSB, fs_LSB),
                                    "b11".U,
                                    reg_mstatus(wpri3_MSB, spp_LSB),
                                    reg_mstatus(mie_MSB, mie_LSB),
                                    reg_mstatus(wpri2_MSB, upie_LSB),
                                    0.U,
                                    reg_mstatus(wpri1_MSB, uie_LSB)),
    ("b" + csr_mret  ).U  ->    Cat( reg_mstatus(sd_MSB, fs_LSB),
                                     0.U,
                                     reg_mstatus(wpri3_MSB, spp_LSB),
                                     1.U,
                                     reg_mstatus(wpri2_MSB, upie_LSB),
                                     reg_mstatus(mpie_MSB, mpie_LSB),
                                     reg_mstatus(wpri1_MSB, uie_LSB)),
    ("b" + csr_csrrw ).U  ->   reg_mstatus_csrrw,
    ("b" + csr_csrrs ).U  ->   reg_mstatus_csrrs,
  ))

  reg_mtvec_csrrw := MuxLookup(io.in.csr_id, reg_mtvec, Array(
    mtvec_id  -> io.in.wdata,
  ))
  reg_mtvec_csrrs := MuxLookup(io.in.csr_id, reg_mtvec, Array(
    mtvec_id  -> (io.in.wdata | reg_mtvec),
  ))
  reg_mtvec := MuxLookup(io.in.csr_op, reg_mtvec, Array(
    ("b" + csr_csrrw ).U  ->   reg_mtvec_csrrw,
    ("b" + csr_csrrs ).U  ->   reg_mtvec_csrrs,
  ))

  io.out.ctrl_csr := MuxLookup(io.in.csr_op, false.B, Array(
    ("b" + csr_ecall ).U  ->   true.B,
    ("b" + csr_mret  ).U  ->   true.B,
  ))

  io.out.csr_pc :=  MuxLookup(io.in.csr_op, 0.U, Array(
    ("b" + csr_ecall ).U  ->   reg_mtvec,
    ("b" + csr_mret  ).U  ->   reg_mepc,
  ))

  // io.out.r_csr  :=  r_csr
  io.out.r_csr   :=  MuxLookup(io.in.csr_id, 0.U, Array(
    mtvec_id    -> reg_mtvec,
    mepc_id     -> reg_mepc,
    mcause_id   -> reg_mcause,
    mstatus_id  -> reg_mstatus,
  ))

  io.out.difftest.mcause  := reg_mcause
  io.out.difftest.mepc    := reg_mepc
  io.out.difftest.mstatus := reg_mstatus
  io.out.difftest.mtvec   := reg_mtvec
}

// object csr_main extends App {
//     emitVerilog(new CSR(), Array("--target-dir", "generated"))
//     // emitVerilog(new WriteSmem(), Array("--target-dir", "generated"))
// }
