package rv32e.core.define
import chisel3._
import chisel3.util._

/**
  * decode info object, arrange from least significant bit (0)
  * to most significant bit (DECODE_INFO_WIDTH-1), the codes here 
  * refers e203's style
  * 
  * decode info from msb to lsb:
  * inst_type, alu_op, src1, src2, reg_wen, mem_wen, ctrl_br, ctrl_jmp
  */
object Dec_Info {
    val MDUOP_WIDTH = 4
    val MDUOP_LSB = 0
    val MDUOP_MSB = MDUOP_LSB + MDUOP_WIDTH - 1
    val mdu_x     = "0000"
    val mdu_mul   = "0001"
    val mdu_mulh  = "0010"
    val mdu_mulhsu= "0011"
    val mdu_mulhu = "0100"
    val mdu_div   = "0101"
    val mdu_divu  = "0110"
    val mdu_rem   = "0111"
    val mdu_remu  = "1000"

    val CSROP_WIDTH = 3
    val CSROP_LSB = MDUOP_MSB + 1
    val CSROP_MSB = CSROP_LSB + CSROP_WIDTH - 1
    val csr_x     = "000"
    val csr_ecall = "001"
    val csr_mret  = "010"
    val csr_csrrw = "011"
    val csr_csrrs = "100"

    val NotImpl_OP_WIDTH = 1
    val NotImpl_OP_LSB = CSROP_MSB + 1
    val NotImpl_OP_MSB = NotImpl_OP_LSB + NotImpl_OP_WIDTH - 1
    val not_impl_yes = "1"
    val not_impl_no = "0"

    /* ebreak inst */
    val EBREAK_OP_WIDTH = 1
    val EBREAK_OP_LSB = NotImpl_OP_MSB + 1
    val EBREAK_OP_MSB = EBREAK_OP_LSB + EBREAK_OP_WIDTH - 1
    val ebreak_yes = "1"
    val ebreak_no  = "0"

    /* memory write enable */
    val MEM_W_OP_WIDTH = 1
    // val MEM_W_OP_LSB = CTRL_BR_MSB + 1
    val MEM_W_OP_LSB = EBREAK_OP_MSB + 1
    val MEM_W_OP_MSB = MEM_W_OP_LSB + MEM_W_OP_WIDTH - 1
    val mem_w_yes = "1" // mem write yes
    val mem_w_no  = "0" // mem write no

    /* register write enable */
    val REG_W_OP_WIDTH = 1
    val REG_W_OP_LSB = MEM_W_OP_MSB + 1
    val REG_W_OP_MSB = REG_W_OP_LSB + REG_W_OP_WIDTH - 1
    val reg_w_yes = "1" // reg write yes
    val reg_w_no  = "0" // reg write no

    /* alu source or other fu source (NOT for bru source, bru source is r(rs1) and r(rs2)) */
    val SRCOP_WIDTH = 2
    val SRC2_LSB = REG_W_OP_MSB + 1
    val SRC2_MSB = SRC2_LSB + SRCOP_WIDTH - 1
    val SRC1_LSB = SRC2_MSB + 1
    val SRC1_MSB = SRC1_LSB + SRCOP_WIDTH - 1
    val src_x       = "00"
    val src_pc      = "01"
    val src_rf      = "10"
    val src_imm     = "11"

    /* alu option */
    val ALUOP_WIDTH = 4
    val ALUOP_LSB   = SRC1_MSB + 1
    val ALUOP_MSB   = ALUOP_LSB + ALUOP_WIDTH - 1
    val alu_x       = "0000" // not use alu
    val alu_add     = "0001"
    val alu_sub     = "0010"
    val alu_and     = "0011"
    val alu_or      = "0100"
    val alu_xor     = "0101"
    val alu_slt     = "0110"
    val alu_sltu    = "0111"
    val alu_sll     = "1000"
    val alu_srl     = "1001"
    val alu_sra     = "1010"

    /* instructions type */
    val INST_TYPEOP_WIDTH = 3
    val INST_TYPEOP_LSB = ALUOP_MSB + 1
    val INST_TYPEOP_MSB = INST_TYPEOP_LSB + INST_TYPEOP_WIDTH - 1
    val r_type  = "000"
    val i_type  = "001"
    val s_type  = "010"
    val b_type  = "011"
    val u_type  = "100"
    val j_type  = "101"
    val no_type = "110"

    /* bru option */
    val BRUOP_WIDTH = 4
    val BRUOP_LSB = INST_TYPEOP_MSB + 1
    val BRUOP_MSB = BRUOP_LSB + BRUOP_WIDTH - 1
    val bru_x    = "0000"
    val bru_jal  = "0001"
    val bru_jalr = "0010"
    val bru_beq  = "0011"
    val bru_bne  = "0100"
    val bru_blt  = "0101"
    val bru_bge  = "0110"
    val bru_bltu = "0111"
    val bru_bgeu = "1000"

    /* lsu option */
    val LSUOP_WIDTH = 4
    val LSUOP_LSB = BRUOP_MSB + 1
    val LSUOP_MSB = LSUOP_LSB + LSUOP_WIDTH - 1
    val lsu_x   = "0000"
    val lsu_lb  = "0001"
    val lsu_lh  = "0010"
    val lsu_lw  = "0011"
    val lsu_lbu = "0100"
    val lsu_lhu = "0101"
    val lsu_sb  = "0110"
    val lsu_sh  = "0111"
    val lsu_sw  = "1000"
    val lsu_lwu = "1001"
    val lsu_ld  = "1010"
    val lsu_sd  = "1011"

    /* function unit type */
    /* differentiating based on "fu" can easily lead to misunderstandings, as
        it may seem that only one "fu" is being used, while in reality, 
        both "bru" and "alu" can be utilized simultaneously. */

     // bru should not listed here, because it can be used simultaneously with alu.
    val FU_TYPEOP_WIDTH = 3
    val FU_TYPEOP_LSB = LSUOP_MSB + 1
    val FU_TYPEOP_MSB = FU_TYPEOP_LSB + FU_TYPEOP_WIDTH - 1
    val fu_x   = "000"
    val fu_alu = "001"
    val fu_mdu = "010"
    val fu_bru = "011"
    val fu_lsu = "100"
    val fu_csr = "101"
}

object CSR_Info {
    val mcause_id   = 0x342.U
    val mepc_id     = 0x341.U
    val mstatus_id  = 0x300.U
    val mtvec_id    = 0x305.U

    // for mstatus rv32
    val uie_W   = 1 // User mode interrupt enable
    val sie_W   = 1 // Supervisor mode interrupt enable
    val wpri1_W = 1 // Reserved (write-preserved) bit 1
    val mie_W   = 1 // Machine mode interrupt enable
    val upie_W  = 1 // User mode previous interrupt enable
    val spie_W  = 1 // Supervisor mode previous interrupt enable
    val wpri2_W = 1 // Reserved (write-preserved) bit 2
    val mpie_W  = 1 // Machine mode previous interrupt enable
    val spp_W   = 1 // Supervisor Previous Privilege mode
    val wpri3_W = 2 // Reserved (write-preserved) bits 3-4
    val mpp_W   = 2 // Machine Previous Privilege mode
    val fs_W    = 2 // Machine mode FPU state
    val xs_W    = 2 // Machine mode extension state
    val mprv_W  = 1 // Modify privilege level when accessing CSRs
    val sum_W   = 1 // Supervisor User Memory Access (S/U)
    val mxr_W   = 1 // Execute-only memory in user mode (X)
    val tvm_W   = 1 // Trap Virtual Memory (TVM)
    val tw_W    = 1 // Timeout Wait
    val tsr_W   = 1 // Trap SRET
    val wpri4_W = 8 // Reserved (write-preserved) bits 5-12
    val sd_W    = 1 // Your custom field

    val uie_LSB     = 0
    val uie_MSB     = uie_LSB + uie_W - 1
    val sie_LSB     = uie_MSB + 1
    val sie_MSB     = sie_LSB + sie_W - 1
    val wpri1_LSB   = sie_MSB + 1
    val wpri1_MSB   = wpri1_LSB + wpri1_W - 1
    val mie_LSB     = wpri1_MSB + 1
    val mie_MSB     = mie_LSB + mie_W - 1
    val upie_LSB    = mie_MSB + 1
    val upie_MSB    = upie_LSB + upie_W - 1
    val spie_LSB    = upie_MSB + 1
    val spie_MSB    = spie_LSB + spie_W - 1
    val wpri2_LSB   = spie_MSB + 1
    val wpri2_MSB   = wpri2_LSB + wpri2_W - 1
    val mpie_LSB    = wpri2_MSB + 1
    val mpie_MSB    = mpie_LSB + mpie_W - 1
    val spp_LSB     = mpie_MSB + 1
    val spp_MSB     = spp_LSB + spp_W - 1
    val wpri3_LSB   = spp_MSB + 1
    val wpri3_MSB   = wpri3_LSB + wpri3_W - 1
    val mpp_LSB     = wpri3_MSB + 1
    val mpp_MSB     = mpp_LSB + mpp_W - 1
    val fs_LSB      = mpp_MSB + 1
    val fs_MSB      = fs_LSB + fs_W - 1
    val xs_LSB      = fs_MSB + 1
    val xs_MSB      = xs_LSB + xs_W - 1
    val mprv_LSB    = xs_MSB + 1
    val mprv_MSB    = mprv_LSB + mprv_W - 1
    val sum_LSB     = mprv_MSB + 1
    val sum_MSB     = sum_LSB + sum_W - 1
    val mxr_LSB     = sum_MSB + 1
    val mxr_MSB     = mxr_LSB + mxr_W - 1
    val tvm_LSB     = mxr_MSB + 1
    val tvm_MSB     = tvm_LSB + tvm_W - 1
    val tw_LSB      = tvm_MSB + 1
    val tw_MSB      = tw_LSB + tw_W - 1
    val tsr_LSB     = tw_MSB + 1
    val tsr_MSB     = tsr_LSB + tsr_W - 1
    val wpri4_LSB   = tsr_MSB + 1
    val wpri4_MSB   = wpri4_LSB + wpri4_W - 1
    val sd_LSB      = wpri4_MSB + 1
    val sd_MSB      = sd_LSB + sd_W - 1
}

