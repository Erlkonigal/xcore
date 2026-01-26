package utility.dpic

import chisel3._
import chisel3.reflect.DataMirror

object TypeMapping {
  // ... (基础类型转换 getSvType, getCppType 保持不变) ...

  def getSvType(data: Data): String = data match {
    case _: UInt | _: SInt =>
      val width = data.getWidth
      if (width <= 0) "bit" else s"bit [${width-1}:0]"
    case _ => "bit"
  }

  def getCppType(data: Data): String = data match {
    case _: UInt | _: SInt =>
      val width = data.getWidth
      if (width <= 8) "uint8_t"
      else if (width <= 16) "uint16_t"
      else if (width <= 32) "uint32_t"
      else if (width <= 64) "uint64_t"
      else s"uint8_t[${(width + 7) / 8}]"
    case _ => "uint8_t"
  }
  
  def getDirectionString(data: Data): String = 
    if (DataMirror.directionOf(data) == ActualDirection.Input) "input" else "output"

  /**
   * 结构体命名：
   * 1. 移除包名 ($符号前的部分)
   * 2. 统一加上前缀 Chisel (如果没有的话)
   */
  def getStructName(bundle: Bundle): String = {
    val className = bundle match {
      case d: DPICBundle => d.className
      case _ => bundle.getClass.getSimpleName.split("\\$").last // 只取最后一段
    }
    val cleanName = if (className.isEmpty) "AnonymousBundle" else className
    // 如果还没前缀 Chisel， 加上
    if (cleanName.startsWith("Chisel")) cleanName else s"Chisel${cleanName}"
  }
  
  // 辅助：获取 Vec 维度
  def getVecDimensions(vec: Vec[_]): (Data, List[Int]) = {
    def rec(d: Data, dims: List[Int]): (Data, List[Int]) = d match {
      case v: Vec[_] => rec(v.head, dims :+ v.length)
      case other => (other, dims)
    }
    rec(vec, Nil)
  }

  // 辅助：判断结构相同
  def isBundleSame(a: Bundle, b: Bundle): Boolean = {
    if (a.elements.size != b.elements.size) return false
    a.elements.zip(b.elements).forall { case ((nA, dA), (nB, dB)) =>
      nA == nB && isDataSame(dA, dB)
    }
  }

  def isDataSame(a: Data, b: Data): Boolean = (a, b) match {
    case (ba: Bundle, bb: Bundle) => isBundleSame(ba, bb)
    case (va: Vec[_], vb: Vec[_]) =>
      val (ea, da) = getVecDimensions(va)
      val (eb, db) = getVecDimensions(vb)
      da == db && isDataSame(ea, eb)
    case _ => getSvType(a) == getSvType(b)
  }
}