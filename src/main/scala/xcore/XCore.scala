package xcore

import chisel3._
import chisel3.util._
import sim.dpic._
import xcore._

class XCore(implicit p: XCoreParams) extends XCoreModule {

  class TestIn extends Bundle {
    val a = UInt(8.W)
    val b = UInt(8.W)
  }

  class TestOut extends Bundle {
    val sum = UInt(8.W)
  }

  val io = IO(new Bundle {
    val in:  TestIn  = Input(new TestIn)
    val out: TestOut = Output(new TestOut)
    val en: Bool    = Input(Bool())
  })

  class TestDPIC extends DPICBundle {
    val clock: Bool    = Input(Bool())
    val reset: Option[Bool]    = None
    val in:    Option[TestIn]  = Some(Input(new TestIn))
    val out:   Option[TestOut] = None

    val en: Option[Bool] = Some(Input(Bool()))
  }

  val dpicIO      = Wire(new TestDPIC)
  val dpicWrapper = Module(new DPICWrapper(dpicIO, "example"))

  dpicIO.clock := clock.asBool
  dpicIO.reset.foreach(_ := reset.asBool)
  dpicIO.en.foreach(_ := io.en)
  dpicIO.in.foreach { in =>
    in.a := io.in.a
    in.b := io.in.b
  }
  dpicIO.out.foreach { out =>
    io.out.sum := out.sum
  }
  io.out.sum := 0.U
  dpicWrapper.io <> dpicIO
}
