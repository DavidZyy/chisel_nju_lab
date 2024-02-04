package learn.multiplier

import chisel3._
import chisel3.util._
import scala.collection.mutable.ListBuffer

import _root_.circt.stage.ChiselStage
import _root_.circt.stage.CIRCTTargetAnnotation
import _root_.circt.stage.CIRCTTarget
import _root_.circt.stage.FirtoolOption
import rv32e.utils.SignExt
import rv32e.utils.ZeroExt

class booth2bit(bitWidth: Int) extends Module {
  val io = IO(new Bundle {
    val operandA = Input(SInt(bitWidth.W))
    val operandB = Input(SInt(bitWidth.W))
    val product = Output(SInt((2 * bitWidth).W))
  })

  val x = io.operandA
  val y = io.operandB

  val product = RegInit(0.U((2*bitWidth).W))

  val tempx = Cat(Fill(bitWidth, x(bitWidth - 1)), x)
  val x_reg = RegInit(UInt((2*bitWidth).W), tempx)
  val tempy = Cat(Fill(bitWidth-1, 0.U), y, 0.U(1.W))
  val y_reg = RegInit(tempy)
  
  val rightmost3bits = Cat(y_reg(2), y_reg(1), y_reg(0))
  val isAdd  = (rightmost3bits === "b001".U) || (rightmost3bits === "b010".U)
  val isSub  = (rightmost3bits === "b101".U) || (rightmost3bits === "b110".U)
  val isAdd2 = (rightmost3bits === "b011".U)
  val isSub2 = (rightmost3bits === "b100".U)

  val value = WireInit(0.U((2*bitWidth).W))

//   when(isSub) {
//     value := ~x_reg + 1.U
//   }.elsewhen(isAdd) {
//     value := x_reg
//   }.elsewhen(isSub2) {
//     value := ~(x_reg << 1) + 1.U
//   }.elsewhen(isAdd2) {
//     value := x_reg << 1
//   } .otherwise {
//     value := 0.U
//   }
// 
//   product := product + value

  when(isSub) {
    product := product - x_reg
  }.elsewhen(isAdd) {
    product := product + x_reg
  }.elsewhen(isSub2) {
    // product := product - x_reg << 1 //wrong
    product := product - (x_reg << 1) //right
  }.elsewhen(isAdd2) {
    product := product + (x_reg << 1)
  } .otherwise {
    product := product
  }

  x_reg := x_reg << 2
  y_reg := y_reg >> 2 // forget shift right 2

  io.product := product.asSInt
}

// compute the product in one cycle, signed
class booth2_1c(bitWidth: Int) extends Module {
  val io = IO(new Bundle {
    val operandA = Input(SInt(bitWidth.W))
    val operandB = Input(SInt(bitWidth.W))
    val product = Output(SInt((2 * bitWidth).W))
  })

  assert(bitWidth % 2 == 0)

  val partialProductsNum = bitWidth / 2
  val partialProductsLength = 2*bitWidth

  // sign-extend to 2*bitWidth bits
  val x = Cat(Fill(bitWidth, io.operandA(bitWidth - 1)), io.operandA)
  val y = Cat(io.operandB, 0.U(1.W)) // y_{-1} = 0, append y_{-1}

  val partialProducts = Wire(Vec(partialProductsNum, UInt(partialProductsLength.W)))

  val map = List(
    "b000".U -> 0.U,
    "b001".U -> x,
    "b010".U -> x, 
    "b011".U -> (x << 1),
    "b100".U -> -(x << 1),
    "b101".U -> -x,
    "b110".U -> -x,
    "b111".U -> 0.U
  )
  
  for(i <- 0 until partialProductsNum) {
    val sel = Cat(y(2*i+2), y(2*i+1), y(2*i+0))
    partialProducts(i) := MuxLookup(sel, 0.U)(map) << 2*i
  }

  val product = partialProducts.reduce(_ +& _)  
  io.product := product.asSInt
}


// compute the product in one cycle, unsigned
class booth2_1c_unsigned(bitWidth: Int) extends Module {
  val io = IO(new Bundle {
    val operandA = Input(UInt(bitWidth.W))
    val operandB = Input(UInt(bitWidth.W))
    val product = Output(UInt((2 * bitWidth).W))
  })

  val trueBitWidth = bitWidth + 2
  val trueOperandA = Cat(0.U(2.W), io.operandA)
  val trueOperandB = Cat(0.U(2.W), io.operandB)

  assert(trueBitWidth % 2 == 0)

  val partialProductsNum = trueBitWidth / 2
  val partialProductsLength = 2*trueBitWidth 

  // sign-extend to 2*bitWidth bits
  val x = Cat(Fill(trueBitWidth , trueOperandA(trueBitWidth  - 1)), trueOperandA)
  val y = Cat(trueOperandB, 0.U(1.W)) // y_{-1} = 0, append y_{-1}

  val partialProducts = Wire(Vec(partialProductsNum, UInt(partialProductsLength.W)))

  val map = List(
    "b000".U -> 0.U,
    "b001".U -> x,
    "b010".U -> x, 
    "b011".U -> (x << 1),
    "b100".U -> -(x << 1),
    "b101".U -> -x,
    "b110".U -> -x,
    "b111".U -> 0.U
  )
  
  for(i <- 0 until partialProductsNum) {
    val sel = Cat(y(2*i+2), y(2*i+1), y(2*i+0))
    partialProducts(i) := MuxLookup(sel, 0.U)(map) << 2*i
  }

  val product =  partialProducts.reduce(_ +& _)
  io.product  := product(2*bitWidth-1, 0)
}

object booth2_1c_unsigned extends App {
  val path = "/home/zhuyangyang/project/CPU/chisel-empty/generated"
  emitVerilog(new booth2_1c_unsigned(bitWidth=32), Array("--target-dir", path))
}

class booth2(bitWidth: Int) extends Module {
  val io = IO(new Bundle {
    val mulSigned = Input(Bool()) 
    val operandA  = Input(UInt(bitWidth.W))
    val operandB  = Input(UInt(bitWidth.W))
    val product   = Output(UInt((2 * bitWidth).W))
  })

  val trueBitWidth = bitWidth + 2
  val trueOperandA = Mux(io.mulSigned, 
                         SignExt(io.operandA, trueBitWidth), 
                         ZeroExt(io.operandA, trueBitWidth))
  val trueOperandB = Mux(io.mulSigned, 
                         SignExt(io.operandB, trueBitWidth), 
                         ZeroExt(io.operandB, trueBitWidth))

  assert(trueBitWidth % 2 == 0)

  val partialProductsNum = trueBitWidth / 2
  val partialProductsLength = 2*trueBitWidth 

  // sign-extend to 2*bitWidth bits
  val x = SignExt(trueOperandA, 2*trueBitWidth)
  val y = Cat(trueOperandB, 0.U(1.W)) // y_{-1} = 0, append y_{-1}

  val partialProducts = Wire(Vec(partialProductsNum, UInt(partialProductsLength.W)))

  val map = List(
    "b000".U -> 0.U,
    "b001".U -> x,
    "b010".U -> x, 
    "b011".U -> (x << 1),
    "b100".U -> -(x << 1),
    "b101".U -> -x,
    "b110".U -> -x,
    "b111".U -> 0.U
  )

  for(i <- 0 until partialProductsNum) {
    val sel = Cat(y(2*i+2), y(2*i+1), y(2*i+0))
    partialProducts(i) := MuxLookup(sel, 0.U)(map) << 2*i
  }

  val product =  partialProducts.reduce(_ +& _)
  io.product  := product(2*bitWidth-1, 0)
}

class csa extends Module {
  val io = IO(new Bundle {
    // val in   = Input(UInt(3.W))
    val in   = Input(Vec(3, UInt(1.W)))
    val cout = Output(Bool()) 
    val s    = Output(Bool())
  })

  val a   = io.in(2)
  val b   = io.in(1)
  val cin = io.in(0)

  io.s := a ^ b ^ cin
  io.cout := a & b | b & cin | a & cin
}

/**
  * 1 bit wallace tree generator 
  * @param srcWidth, the count of partial product which should be sum up
  */
class wallaceGenerator(srcWidth: Int) extends Module {
  var curSrcWidth = srcWidth // current layer input width
  var remainSrcWidth = 0
  var casCntLevel: Int = 0 // current level's csa counts
  var list: ListBuffer[Int] = ListBuffer.empty[Int]

  assert(curSrcWidth >= 2)
  while (curSrcWidth != 2) {
    casCntLevel =  curSrcWidth / 3
    remainSrcWidth =  curSrcWidth % 3
    list.addOne(casCntLevel)

    curSrcWidth = 2*casCntLevel + remainSrcWidth // next layer input wire count
  }

  val csaCntTotal = list.sum

  val io = IO(new Bundle {
    val src_in = Input(UInt(srcWidth.W)) // from bottom of tree
    val cin    = Input(UInt((csaCntTotal-1).W)) // from right of tree
    val cout_group = Output(UInt((csaCntTotal-1).W)) // to left of tree
    val cout = Output(Bool()) // to top of tree
    val s    = Output(Bool()) // to top of tree
  })

  val firstLayerInput = io.src_in.asBools

  // one layer one recursive
  def recursiveAdd(layerInput: Seq[Bool], cin: Seq[Bool], csaCntSumUp: Int, depth: Int): (Seq[Bool], Seq[Bool], Seq[Bool]) = {
    var cout_group = Seq[Bool]()
    var cout       = Seq[Bool]()
    var s          = Seq[Bool]()

    var NextlayerInput = Seq[Bool]()

    val csaCnt = layerInput.size / 3
    val remainSrcWidth = layerInput.size % 3

    val csaInstances   = Seq.fill(csaCnt)(Module(new csa))

    for (i <- 0 until csaCnt) {
      val startIndex = layerInput.size - 3*(i+1)
      val endIndex = layerInput.size - 3*i
      csaInstances(i).io.in := layerInput.slice(startIndex, endIndex)
      cout_group = cout_group.appended(csaInstances(i).io.cout)
      NextlayerInput = NextlayerInput.appended(csaInstances(i).io.s)
    }

    NextlayerInput = NextlayerInput ++ layerInput.slice(0, layerInput.size - 3*csaCnt).reverse
    NextlayerInput = NextlayerInput ++ cin.slice(csaCntSumUp, csaCntSumUp+csaCnt)

    // println("depth: " + depth + ", size of NextlayerInput: " + NextlayerInput.size)

    val (cout_groupNext, coutNext, sNext) =  
    if(NextlayerInput.size <= 2) {
      (Seq[Bool](), Seq(csaInstances(0).io.cout), Seq(csaInstances(0).io.s))
    } else {
      recursiveAdd(NextlayerInput, cin, csaCntSumUp+csaCnt, depth+1)
    }

    (cout_group ++ cout_groupNext, coutNext, sNext)
  }

  val (cout_group, cout, s) = recursiveAdd(io.src_in.asBools, io.cin.asBools, 0, 0)

  io.cout_group := Cat(cout_group.reverse)
  io.cout := Cat(cout.reverse)
  io.s := Cat(s.reverse)
}

object wallaceGeneratorMain extends App {
  val path = "/home/zhuyangyang/project/CPU/chisel-empty/generated"
    val firtoolOptions = Seq(
        FirtoolOption("--disable-all-randomization"),
        FirtoolOption("--lowering-options=disallowLocalVariables, locationInfoStyle=none"),
        // FirtoolOption("--lowering-options=locationInfoStyle=none")
    )
  emitVerilog(new wallaceGenerator(srcWidth = 4), Array("--target-dir", path), firtoolOptions)
}

class partialProductsGenerator(bitWidth: Int) extends Module {
  assert(bitWidth % 2 == 0)
  val trueBitWidth = bitWidth + 2
  val partialProductsNum = trueBitWidth / 2
  val partialProductsLength = 2*trueBitWidth 

  val io = IO(new Bundle {
    val mulSigned = Input(Bool()) 
    val operandA  = Input(UInt(bitWidth.W))
    val operandB  = Input(UInt(bitWidth.W))
    val partialProducts = Output(Vec(partialProductsNum, UInt(partialProductsLength.W)))
  })

  val trueOperandA = Mux(io.mulSigned, 
                         SignExt(io.operandA, trueBitWidth), 
                         ZeroExt(io.operandA, trueBitWidth))
  val trueOperandB = Mux(io.mulSigned, 
                         SignExt(io.operandB, trueBitWidth), 
                         ZeroExt(io.operandB, trueBitWidth))

  // sign-extend to 2*bitWidth bits
  val x = SignExt(trueOperandA, 2*trueBitWidth)
  val y = Cat(trueOperandB, 0.U(1.W)) // y_{-1} = 0, append y_{-1}

  val partialProducts = Wire(Vec(partialProductsNum, UInt(partialProductsLength.W)))

  val map = List(
    "b000".U -> 0.U,
    "b001".U -> x,
    "b010".U -> x, 
    "b011".U -> (x << 1),
    "b100".U -> -(x << 1),
    "b101".U -> -x,
    "b110".U -> -x,
    "b111".U -> 0.U
  )

  for(i <- 0 until partialProductsNum) {
    val sel = Cat(y(2*i+2), y(2*i+1), y(2*i+0))
    partialProducts(i) := MuxLookup(sel, 0.U)(map) << 2*i
  }

  io.partialProducts := partialProducts
}

class partialProductsSwitch(bitWidth: Int) extends Module {
  assert(bitWidth % 2 == 0)
  val trueBitWidth = bitWidth + 2
  val partialProductsNum = trueBitWidth / 2
  val partialProductsLength = 2*trueBitWidth 

  val io = IO (new Bundle {
    val in  = Input(Vec(partialProductsNum, UInt(partialProductsLength.W)))
    val out = Output(Vec(partialProductsLength, UInt(partialProductsNum.W)))
  })

   // Transpose the input to the output
  for (i <- 0 until partialProductsLength) {
    // chatgpt give me this, but it doesnot work, it seems that we can not assign to 1 bit of a UInt in chisel.
    // for (j <- 0 until partialProductsNum) {
    //   io.out(i)(j) := io.in(j)(i)
    // }
    val concatenated = Cat(io.in.map(_(i)).reverse)
    io.out(i) := concatenated
  }
}

/**
  * 
  *
  * @param bitWidth
  */
class myMultipier(bitWidth: Int) extends Module {
  val io = IO(new Bundle {
    val mulSigned = Input(Bool()) 
    val operandA  = Input(UInt(bitWidth.W))
    val operandB  = Input(UInt(bitWidth.W))
    val product   = Output(UInt((2 * bitWidth).W))
    // val PPGprodeuct = Output(UInt((2 * bitWidth).W))  // for debug
  })

  assert(bitWidth % 2 == 0)
  val trueBitWidth = bitWidth + 2
  val partialProductsNum = trueBitWidth / 2
  val partialProductsLength = 2*trueBitWidth 

  val PPG = Module(new partialProductsGenerator(bitWidth))
  PPG.io.mulSigned := io.mulSigned
  PPG.io.operandA  := io.operandA
  PPG.io.operandB  := io.operandB

  // io.PPGprodeuct := PPG.io.partialProducts.reduce(_ +& _)(2*bitWidth-1, 0)

  val PPS = Module(new partialProductsSwitch(bitWidth))
  PPS.io.in := PPG.io.partialProducts

  val wallaceTrees = Seq.fill(partialProductsLength)(Module(new wallaceGenerator(partialProductsNum))) 
  wallaceTrees(0).io.cin := DontCare
  for(i <- 0 until partialProductsLength) {
    wallaceTrees(i).io.src_in := PPS.io.out(i)
    if(i>0) {
      wallaceTrees(i).io.cin := wallaceTrees(i-1).io.cout_group
    }
  }

  // Concatenate all Cout outputs
  val allCout = Cat(wallaceTrees.map(_.io.cout).reverse)

  // Concatenate all s outputs
  val allS = Cat(wallaceTrees.map(_.io.s).reverse)

  io.product := (allS + (allCout<<1))(2*bitWidth-1, 0)
}

object myMultipierMain extends App {
  val path = "/home/zhuyangyang/project/CPU/chisel-empty/generated"
  val firtoolOptions = Seq (
      FirtoolOption("--disable-all-randomization"),
      FirtoolOption("--lowering-options=disallowLocalVariables, locationInfoStyle=none"),
      // FirtoolOption("--lowering-options=locationInfoStyle=none")
  )
  emitVerilog(new myMultipier(bitWidth = 4), Array("--target-dir", path), firtoolOptions)
}

