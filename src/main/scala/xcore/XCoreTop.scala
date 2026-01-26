package xcore

import _root_.circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import utility.dpic.GenController

object XCoreSimTop extends App {
  def parseArgs(args: List[String], map: Map[String, String] = Map()): Map[String, String] = {
    args match {
      case Nil => map
      case "--target-dir" :: value :: tail => 
        parseArgs(tail, map + ("target" -> value))
      case "--generated-dir" :: value :: tail => 
        parseArgs(tail, map + ("generated" -> value))
      case unknown :: tail => 
        println(s"Unknown argument: $unknown")
        parseArgs(tail, map)
    }
  }

  implicit val p: XCoreParams = XCoreParams()
  ChiselStage.emitSystemVerilogFile(
    new XCore,
    firtoolOpts = Array(
      "-disable-all-randomization",
      "-strip-debug-info",
      "-default-layer-specialization=enable"
    ),
    args = Array(
      "--target-dir",
      parseArgs(args.toList).getOrElse("target", "build/rtl")
    )
  )
  GenController.writeAllSources(
    parseArgs(args.toList).getOrElse("generated", "build/generated-src")
  )
}
