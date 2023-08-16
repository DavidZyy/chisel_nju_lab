package learn

import chisel3._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._

class SimpleDecoder extends Module {
  val table = TruthTable(
    Map(
      BitPat("b001") -> BitPat("b?"),
      BitPat("b010") -> BitPat("b?"),
      BitPat("b100") -> BitPat("b1"),
      BitPat("b101") -> BitPat("b1"),
      BitPat("b111") -> BitPat("b1")
    ),
    BitPat("b0"))
  val input = IO(Input(UInt(3.W)))
  val output = IO(Output(UInt(1.W)))
  output := decoder(input, table)
}

// case class Pattern(val name: String, val code: BigInt) extends DecodePattern {
//   def bitPat: BitPat = BitPat("b" + code.toString(2))
// }
// 
// object NameContainsAdd extends BoolDecodeField[Pattern] {
//   def name = "name contains 'add'"
//   def genTable(i: Pattern) = if (i.name.contains("add")) y else n
// }
// 
// 
// class SimpleDecodeTable extends Module {
//   val allPossibleInputs = Seq(Pattern("addi", BigInt("0x2")) /* can be generated */)
//   val decodeTable = new DecodeTable(allPossibleInputs, Seq(NameContainsAdd))
//   
//   val input = IO(Input(UInt(4.W)))
//   val isAddType = IO(Output(Bool()))
//   val decodeResult = decodeTable.decode(input)
//   isAddType := decodeResult(NameContainsAdd)
// }