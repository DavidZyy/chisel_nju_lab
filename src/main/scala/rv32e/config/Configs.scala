package rv32e.config

import chisel3._

object Configs {
    val ADDR_WIDTH      =   32
    val ADDR_BYTE_WIDTH =   4

    val DATA_WIDTH      =   32

    val INST_WIDTH      =   32
    val INST_BYTE_WIDTH =   4

    val MEM_INST_SIZE   =   1024
    val MEM_DATA_SIZE   =   2048 

    val START_ADDR: Long =  0x80000000L

    /* 2^4 = 16 general registers */
    val REG_OP_WIDTH = 5
}

/**
  * decode info object, arrange from least significant bit (0)
  * to most significant bit (DECODE_INFO_WIDTH-1), the codes here 
  * refers e203's style
  * 
  * decode info from msb to lsb:
  * inst_type, alu_op, src1, src2, reg_wen, mem_wen, ctrl_br, ctrl_jmp
  */
object Dec_Info {
//     /* is branch inst ?*/
//     val CTRL_JMP_WIDTH = 1
//     val CTRL_JMP_LSB = 0
//     val CTRL_JMP_MSB = CTRL_JMP_LSB + CTRL_JMP_WIDTH - 1
//     val ctrl_jmp_yes = "1"
//     val ctrl_jmp_no  = "0"
// 
//     /* is branch inst ? */
//     val CTRL_BR_WIDTH = 1
//     val CTRL_BR_LSB   = CTRL_JMP_MSB + 1
//     val CTRL_BR_MSB   = CTRL_BR_LSB + CTRL_BR_WIDTH - 1
//     val ctrl_br_yes = "1"
//     val ctrl_br_no  = "0"

    /* ebreak inst */
    val EBREAK_OP_WIDTH = 1
    val EBREAK_OP_LSB = 0
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
    val fu_alu = "000"

    // val DECODE_INFO_WIDTH = TYPEOP_MSB + 1
}
