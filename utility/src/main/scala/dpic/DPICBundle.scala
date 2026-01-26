package utility.dpic

import chisel3._
import chisel3.reflect.DataMirror // 引入反射工具
import chisel3.ActualDirection     // 引入方向定义

trait DPICRequirements {
  val in: Option[Data]
  val out: Option[Data]
  val en: Option[Bool]
  val reset: Option[Bool]
  val clock: Bool
}

abstract class DPICBundle extends Bundle with DPICRequirements { 
  override def className: String = this.getClass.getSimpleName.replace("$", "")

  /**
   * 检查方向是否符合 DPI 接口定义：
   * - in, en, reset, clock 必须是 Input
   * - out 必须是 Output
   */
  def validateDirections(): Unit = {
    // 辅助检查函数
    def checkDir(node: Option[Data], expected: ActualDirection, name: String): Unit = {
      node.foreach { d =>
        val dir = DataMirror.directionOf(d)
        require(dir == expected, 
          s"\n[DPIC Error] Signal mapped to '$name' in bundle '$className' has wrong direction.\n" +
          s"  Expected: $expected\n" +
          s"  Actual:   $dir\n" +
          s"  Signal:   $d"
        )
      }
    }

    // 1. 检查 clock (必须是 Input)
    // 注意：Clock 类型本身不带方向，只有被 Input(Clock()) 包裹才有 ActualDirection
    require(DataMirror.directionOf(clock) == ActualDirection.Input, 
      s"[DPIC Error] 'clock' in '$className' must be Input(Clock()).")

    // 2. 检查可选信号
    checkDir(in,    ActualDirection.Input,  "in")
    checkDir(out,   ActualDirection.Output, "out")
    checkDir(en,    ActualDirection.Input,  "en")
    checkDir(reset, ActualDirection.Input,  "reset")
  }
}