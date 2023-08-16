package empty
import chisel3._
import chisel3.util._
import chisel3.util.BitPat

object GlobalStrings {
  val sharedString: String = "This is a shared string"
}

class MyClass {
  import GlobalStrings.sharedString

  def printSharedString(): Unit = {
    println(sharedString)
  }
}

object AnotherObject {
//   import GlobalStrings.sharedString

  def printSharedString(): Unit = {
    // println(sharedString)
    println(GlobalStrings.sharedString)
  }
}


object MyEnum extends Enumeration {
  val StateA, StateB, StateC = Value
  // val StateA, StateB, StateC = 0
}

object main extends App {

   def imm_i(inst: Long): Long= {
        val imm = (inst >> 20) & 0xFFF
        val signBit = imm >> 11
        if (signBit == 1) {
          imm | 0xFFFFF000L
        } else {
          imm
        }
    }

  val inst = 0x800007b7L
  // println(inst)
  // println(imm_i(inst))
  // println(imm_i(inst(11)))
  val a = imm_i(inst)
  println(a.U)
  println(a)
}

case class Person(name: String, age: Int)

object Main_cclass extends App {
  val person1 = Person("Alice", 25)
  val person2 = Person("Bob", 30)

  println(person1) // Output: Person(Alice,25)
  println(person2) // Output: Person(Bob,30)

  val updatedPerson = person1.copy(age = 26)
  println(updatedPerson) // Output: Person(Alice,26)

  person1 match {
    case Person(name, age) => println(s"Name: $name, Age: $age")
  }
}


object Dec_Info {
    val MEM_W_OP_WIDTH = 1
    val MEM_W_OP_LSB = 0
    val MEM_W_OP_MSB = MEM_W_OP_LSB + MEM_W_OP_WIDTH - 1
    val mem_w_yes = "1" // mem write yes
    val mem_w_no  = "0" // mem write no

    val REG_W_OP_WIDTH = 1
    val REG_W_OP_LSB = MEM_W_OP_MSB + 1
    val REG_W_OP_MSB = REG_W_OP_LSB + REG_W_OP_WIDTH - 1
    val reg_w_yes = "1" // reg write yes
    val reg_w_no  = "0" // reg write no

    /* alu source */
    val SRCOP_WIDTH = 2
    val SRC2_LSB = REG_W_OP_MSB + 1
    val SRC2_MSB = SRC2_LSB + SRCOP_WIDTH - 1
    val SRC1_LSB = SRC2_MSB + 1
    val SRC1_MSB = SRC1_LSB + SRCOP_WIDTH - 1
    val src_x       = "00"
    val src_pc      = "01"
    val src_rf      = "10"
    val src_imm     = "11"

    /* alulu */
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

    val TYPEOP_WIDTH = 3
    val TYPEOP_LSB = ALUOP_MSB + 1
    val TYPEOP_MSB = TYPEOP_LSB + TYPEOP_WIDTH - 1
    val r_type  = "b000"
    val i_type  = "b001"
    val s_type  = "b010"
    val b_type  = "b011"
    val u_type  = "b100"
    val j_type  = "b101"
    val no_type = "b110"

    // val DECODE_INFO_WIDTH = TYPEOP_WIDTH + ALUOP_WIDTH + 2*SRCOP_WIDTH + REG_W_OP_WIDTH + MEM_W_OP_WIDTH
    val DECODE_INFO_WIDTH = TYPEOP_MSB + 1
}

object dec_main extends App {
  println(Dec_Info.TYPEOP_MSB)
  println(Dec_Info.TYPEOP_LSB)
}