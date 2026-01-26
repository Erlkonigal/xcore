package xcore

import chisel3._
import xcore._
abstract class XCoreBundle(implicit val p: XCoreParams) extends Bundle
    with HasXCoreParams {}

abstract class XCoreModule(implicit val p: XCoreParams) extends Module
    with HasXCoreParams {}
