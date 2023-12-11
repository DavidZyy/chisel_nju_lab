package rv32e

import chisel3._
import chisel3.util._

import rv32e.config.Configs._
import rv32e.config.Axi_Configs._
import rv32e.define.Inst._
import rv32e.bus._

class IFU_in_class extends Bundle {
    val ctrl_br         =   Input(Bool())   // come from bru result
    val addr_target     =   Input(UInt(ADDR_WIDTH.W)) // come from alu result
    val ctrl_csr        =   Input(Bool()) 
    val excpt_addr      =   Input(UInt(ADDR_WIDTH.W))
}

class IFU_out_class extends Bundle {
    val cur_pc          =   Output(UInt(ADDR_WIDTH.W))
}

class IFUIO extends Bundle {
    val in  = (new IFU_in_class)
    val out = (new IFU_out_class)
}

// IFU with axi-lite interface connected to mem
class IFU extends Module {
    val to_IDU   = IO(Decoupled(new IFU2IDU_bus)) // only to IDU signal
    val from_EXU = IO(Flipped(Decoupled(new EXU2IFU_bus)))
    val from_WBU = IO(Flipped(Decoupled(new WBU2IFU_bus)))
    val axi      = IO(new AXILiteIO_master)

    val reg_PC  = RegInit(UInt(ADDR_WIDTH.W), START_ADDR.U)
    val next_PC = Wire(UInt(ADDR_WIDTH.W))

    when (from_EXU.bits.bru_ctrl_br) {
        next_PC := from_EXU.bits.bru_addr
    } .elsewhen (from_EXU.bits.csr_ctrl_br) {
        next_PC := from_EXU.bits.csr_addr
    } .otherwise {
        next_PC := reg_PC + ADDR_BYTE.U
    }

    // in some cycle, it has some instruction issue
    reg_PC := Mux(from_WBU.fire, next_PC, reg_PC)

    // state_ifu machine
    val s_rq :: s_wait_data :: s_wait_WB :: Nil = Enum(3)
    val state_ifu = RegInit(s_rq)
    state_ifu := MuxLookup(state_ifu, s_rq)(List(
        s_rq      ->  Mux(axi.ar.fire,   s_wait_data,    s_rq),
        s_wait_data ->  Mux(axi.r.fire,    s_wait_WB,      s_wait_data),
        s_wait_WB   ->  Mux(from_WBU.fire, s_rq,         s_wait_WB)
    ))

    // axi master signals
    axi.ar.valid     := MuxLookup(state_ifu, false.B)(List( s_rq      ->  true.B))
    axi.ar.bits.addr := reg_PC
    axi.r.ready      := MuxLookup(state_ifu, false.B)(List( s_wait_data ->  true.B))
    axi.aw.valid     := false.B
    axi.aw.bits.addr := 0.U
    axi.w.valid      := false.B
    axi.w.bits.data  := 0.U
    axi.w.bits.strb  := 0.U
    axi.b.ready      := false.B

    // to IDU signals
    to_IDU.valid     := MuxLookup(state_ifu, false.B)(List( s_wait_WB   ->  true.B))
    to_IDU.bits.inst := Mux(to_IDU.fire, axi.r.bits.data, NOP.U) // if not ready, transfer nop inst
    to_IDU.bits.pc   := reg_PC

    // from EXU signals
    from_EXU.ready  := MuxLookup(state_ifu, false.B)(List( s_wait_WB   ->  true.B))

    // from WBU signals
    from_WBU.ready  := MuxLookup(state_ifu, false.B)(List( s_wait_WB   ->  true.B))
}

// ifu with axi interface connected to mem
class IFU_axi extends Module {
    val to_IDU   = IO(Decoupled(new IFU2IDU_bus)) // only to IDU signal
    val from_EXU = IO(Flipped(Decoupled(new EXU2IFU_bus)))
    val from_WBU = IO(Flipped(Decoupled(new WBU2IFU_bus)))
    val axi      = IO(new AXIIO)

    val reg_PC  = RegInit(UInt(ADDR_WIDTH.W), START_ADDR.U)
    val next_PC = Wire(UInt(ADDR_WIDTH.W))

    when (from_EXU.bits.bru_ctrl_br) {
        next_PC := from_EXU.bits.bru_addr
    } .elsewhen (from_EXU.bits.csr_ctrl_br) {
        next_PC := from_EXU.bits.csr_addr
    } .otherwise {
        next_PC := reg_PC + ADDR_BYTE.U
    }

    // in some cycle, it has some instruction issue
    reg_PC := Mux(from_WBU.fire, next_PC, reg_PC)

    // state_ifu machine
    val s_rq :: s_wait_data :: s_wait_WB :: Nil = Enum(3)
    val state_ifu = RegInit(s_rq)
    state_ifu := MuxLookup(state_ifu, s_rq)(List(
        s_rq        ->  Mux(axi.ar.fire,   s_wait_data, s_rq),
        s_wait_data ->  Mux(axi.r.fire,    s_wait_WB,   s_wait_data),
        s_wait_WB   ->  Mux(from_WBU.fire, s_rq,        s_wait_WB)
    ))

    // axi master signals
    axi.ar.valid      := MuxLookup(state_ifu, false.B)(List(s_rq -> true.B))
    axi.ar.bits.addr  := reg_PC
    axi.ar.bits.size  := MuxLookup(state_ifu , 0.U)(List(s_rq -> DATA_WIDTH.U))
    axi.ar.bits.len   := 0.U
    axi.ar.bits.burst := INCR.U
    axi.r.ready       := MuxLookup(state_ifu, false.B)(List(s_wait_data ->  true.B))
    axi.aw.valid      := false.B
    axi.aw.bits.addr  := 0.U
    axi.aw.bits.size  := 0.U
    axi.aw.bits.len   := 0.U
    axi.aw.bits.burst := 0.U
    axi.w.valid       := false.B
    axi.w.bits.data   := 0.U
    axi.w.bits.strb   := 0.U
    axi.w.bits.last   := false.B
    axi.b.ready       := false.B

    // to IDU signals
    to_IDU.valid     := MuxLookup(state_ifu, false.B)(List(s_wait_WB   ->  true.B))
    to_IDU.bits.inst := Mux(to_IDU.fire, axi.r.bits.data, NOP.U) // if not ready, transfer nop inst
    to_IDU.bits.pc   := reg_PC

    // from EXU signals
    from_EXU.ready  := MuxLookup(state_ifu, false.B)(List(s_wait_WB   ->  true.B))

    // from WBU signals
    from_WBU.ready  := MuxLookup(state_ifu, false.B)(List(s_wait_WB   ->  true.B))
}

// ifu connects to icache
class IFU_cache extends Module {
    val to_IDU     = IO(Decoupled(new IFU2IDU_bus)) // only to IDU signal
    val from_EXU   = IO(Flipped(Decoupled(new EXU2IFU_bus)))
    val from_WBU   = IO(Flipped(Decoupled(new WBU2IFU_bus)))
    val to_cache   = IO(Decoupled(new IFU2Cache_bus))
    val from_cache = IO(Flipped(Decoupled((new Cache2IFU_bus))))

    val reg_PC  = RegInit(UInt(ADDR_WIDTH.W), START_ADDR.U)
    val next_PC = Wire(UInt(ADDR_WIDTH.W))

    when (from_EXU.bits.bru_ctrl_br) {
        next_PC := from_EXU.bits.bru_addr
    } .elsewhen (from_EXU.bits.csr_ctrl_br) {
        next_PC := from_EXU.bits.csr_addr
    } .otherwise {
        next_PC := reg_PC + ADDR_BYTE.U
    }

    // in some cycle, it has some instruction issue
    reg_PC := Mux(from_WBU.fire, next_PC, reg_PC)

    // state_ifu machine
    val s_rq :: s_wait_data :: s_wait_WB :: Nil = Enum(3)
    val state_ifu = RegInit(s_rq)
    state_ifu := MuxLookup(state_ifu, s_rq)(List(
        s_rq      ->  Mux(to_cache.fire,   s_wait_data,    s_rq),
        s_wait_data ->  Mux(from_cache.fire,    s_wait_WB,      s_wait_data),
        s_wait_WB   ->  Mux(from_WBU.fire, s_rq,         s_wait_WB)
    ))

    // axi master signals
    to_cache.valid     := MuxLookup(state_ifu, false.B)(List( s_rq      ->  true.B))
    to_cache.bits.addr := reg_PC
    from_cache.ready      := MuxLookup(state_ifu, false.B)(List( s_wait_data ->  true.B))

    // to IDU signals
    to_IDU.valid     := MuxLookup(state_ifu, false.B)(List( s_wait_WB   ->  true.B))
    to_IDU.bits.inst := Mux(to_IDU.fire, from_cache.bits.data, NOP.U) // if not ready, transfer nop inst
    to_IDU.bits.pc   := reg_PC

    // from EXU signals
    from_EXU.ready  := MuxLookup(state_ifu, false.B)(List( s_wait_WB   ->  true.B))

    // from WBU signals
    from_WBU.ready  := MuxLookup(state_ifu, false.B)(List( s_wait_WB   ->  true.B))
}

class IFU_simpleBus extends Module {
    val to_IDU     = IO(Decoupled(new IFU2IDU_bus)) // only to IDU signal
    val from_EXU   = IO(Flipped(Decoupled(new EXU2IFU_bus)))
    val from_WBU   = IO(Flipped(Decoupled(new WBU2IFU_bus)))
    val to_mem     = IO(new SimpleBus)

    val reg_PC  = RegInit(UInt(ADDR_WIDTH.W), START_ADDR.U)
    val next_PC = Wire(UInt(ADDR_WIDTH.W))

    when (from_EXU.bits.bru_ctrl_br) {
        next_PC := from_EXU.bits.bru_addr
    } .elsewhen (from_EXU.bits.csr_ctrl_br) {
        next_PC := from_EXU.bits.csr_addr
    } .otherwise {
        next_PC := reg_PC + ADDR_BYTE.U
    }

    // in some cycle, it has some instruction issue
    reg_PC := Mux(from_WBU.fire, next_PC, reg_PC)

    // state_ifu machine
    val s_rq :: s_wait_data :: s_wait_WB :: Nil = Enum(3)
    val state_ifu = RegInit(s_rq)
    state_ifu := MuxLookup(state_ifu, s_rq)(List(
        s_rq        ->  Mux(to_mem.req.fire,   s_wait_data,  s_rq),
        s_wait_data ->  Mux(to_mem.resp.fire, s_wait_WB,    s_wait_data),
        s_wait_WB   ->  Mux(from_WBU.fire,   s_rq,         s_wait_WB)
    ))

    //  simple signals
    to_mem.req.valid      := MuxLookup(state_ifu, false.B)(List( s_rq      ->  true.B))
    to_mem.req.bits.addr  := reg_PC
    to_mem.req.bits.cmd   := SimpleBusCmd.read
    to_mem.req.bits.wdata := DontCare
    to_mem.req.bits.wmask := DontCare
    to_mem.resp.ready     := MuxLookup(state_ifu, false.B)(List( s_wait_data ->  true.B))

    // to IDU signals
    // to_IDU.valid     := MuxLookup(state_ifu, false.B)(List( s_wait_WB   ->  true.B))
    to_IDU.valid     := to_mem.resp.fire
    // to_IDU.bits.inst := Mux(to_IDU.fire, to_mem.resp.bits.rdata, NOP.U) // if not ready, transfer nop inst
    to_IDU.bits.inst := Mux(to_IDU.fire, to_mem.resp.bits.rdata, 0.U) // if not ready, transfer nop inst
    to_IDU.bits.pc   := reg_PC

    // from EXU signals
    from_EXU.ready  := MuxLookup(state_ifu, false.B)(List( s_wait_WB   ->  true.B))

    // from WBU signals
    from_WBU.ready  := MuxLookup(state_ifu, false.B)(List( s_wait_WB   ->  true.B))
}

class IFU_pipeline extends Module {
    val to_IDU     = IO(Decoupled(new IFU2IDU_bus)) // only to IDU signal
    val from_EXU   = IO(Flipped(Decoupled(new EXU2IFU_bus)))
    // val from_WBU   = IO(Flipped(Decoupled(new WBU2IFU_bus)))
    val to_mem     = IO(new SimpleBus)

    val reg_PC  = RegInit(UInt(ADDR_WIDTH.W), START_ADDR.U)
    val next_PC = Wire(UInt(ADDR_WIDTH.W))

    when (from_EXU.bits.bru_ctrl_br) {
        next_PC := from_EXU.bits.bru_addr
    } .elsewhen (from_EXU.bits.csr_ctrl_br) {
        next_PC := from_EXU.bits.csr_addr
    } .otherwise {
        next_PC := reg_PC + ADDR_BYTE.U
    }

    // in some cycle, it has some instruction issue
    // reg_PC := Mux(from_WBU.fire, next_PC, reg_PC)
    reg_PC := Mux(to_mem.resp.fire && to_IDU.fire, next_PC, reg_PC)

    // state_ifu machine
    // val s_rq :: s_wait_data :: s_wait_WB :: Nil = Enum(3)
    // val state_ifu = RegInit(s_rq)
    // state_ifu := MuxLookup(state_ifu, s_rq)(List(
    //     s_rq        ->  Mux(to_mem.req.fire,   s_wait_data,  s_rq),
    //     s_wait_data ->  Mux(to_mem.resp.fire, s_wait_WB,    s_wait_data),
    //     s_wait_WB   ->  Mux(from_WBU.fire,   s_rq,         s_wait_WB)
    // ))

    //  simple signals
    // to_mem.req.valid      := MuxLookup(state_ifu, false.B)(List( s_rq      ->  true.B))
    to_mem.req.valid      := to_IDU.ready
    to_mem.req.bits.addr  := reg_PC
    to_mem.req.bits.cmd   := SimpleBusCmd.read
    to_mem.req.bits.wdata := DontCare
    to_mem.req.bits.wmask := DontCare
    // to_mem.resp.ready     := MuxLookup(state_ifu, false.B)(List( s_wait_data ->  true.B))
    to_mem.resp.ready     := to_IDU.ready

    // to IDU signals
    // to_IDU.valid     := to_mem.resp.valid
    // to_IDU.valid     := MuxLookup(state_ifu, false.B)(List( s_wait_WB   ->  true.B))
    to_IDU.valid     := to_mem.resp.fire
    to_IDU.bits.inst := Mux(to_IDU.fire, to_mem.resp.bits.rdata, 0.U) // if not ready, transfer nop inst
    to_IDU.bits.pc   := reg_PC

    // from EXU signals
    // from_EXU.ready  := MuxLookup(state_ifu, false.B)(List( s_wait_WB   ->  true.B))
    from_EXU.ready  := true.B

    // from WBU signals
    // from_WBU.ready  := MuxLookup(state_ifu, false.B)(List( s_wait_WB   ->  true.B))
    // from_WBU.ready  := true.B
}

