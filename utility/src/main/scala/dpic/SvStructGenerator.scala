package utility.dpic

import chisel3._
import scala.collection.mutable.ListBuffer

object SVStructGenerator {

  // 生成所有 typedef struct
  def generateSvStructs(data: Data, verilog: ListBuffer[String], collected: Seq[(String, Bundle)]): Unit = {
    collected.foreach { case (structName, bundle) =>
      verilog += s"typedef struct packed {"
      bundle.elements.foreach { case (fieldName, field) =>
        val decl = getFieldDeclaration(field, fieldName, collected)
        verilog += s"  $decl"
      }
      verilog += s"} $structName;"
      verilog += ""
    }
  }

  private def getFieldDeclaration(field: Data, name: String, collected: Seq[(String, Bundle)]): String = {
    field match {
      case b: Bundle =>
        val typeName = StructCollector.lookupName(b, collected)
        s"$typeName $name;"
      case vec: Vec[_] =>
        val (elem, dims) = TypeMapping.getVecDimensions(vec)
        val dimStr = dims.reverse.map(d => s"[${d-1}:0]").mkString(" ") // SV 数组定义 [N-1:0]
        elem match {
          case eb: Bundle =>
             val typeName = StructCollector.lookupName(eb, collected)
             s"$typeName $dimStr $name;"
          case _ =>
             val typeName = TypeMapping.getSvType(elem)
             s"$typeName $dimStr $name;"
        }
      case _ =>
        s"${TypeMapping.getSvType(field)} $name;"
    }
  }

  // 生成 module 端口列表 (a_b, c_d, ...)
  def generatePortsList(data: Data, verilog: ListBuffer[String]): Unit = {
    val ports = ListBuffer.empty[String]
    FlattenUtils.traverse(data, (flatName, _, _) => {
      ports += s"$flatName" // 扁平化名称
    })
    if (ports.nonEmpty) {
      verilog += ("  " + ports.mkString(",\n  "))
    }
  }

  // 生成 module 端口详细定义 (input [x:0] a_b;)
  def generatePortDeclarations(data: Data, verilog: ListBuffer[String]): Unit = {
    FlattenUtils.traverse(data, (flatName, _, leaf) => {
      val dir = TypeMapping.getDirectionString(leaf)
      val width = leaf.getWidth
      val size = if (width > 1) s" [${width-1}:0]" else ""
      verilog += s"  $dir$size $flatName;"
    })
  }

  // 生成 Struct 和 Flattened Ports 之间的 Assign
  def generateStructAssignment(data: Data, verilog: ListBuffer[String], structObjName: String): Unit = {
    val isInput = TypeMapping.getDirectionString(data) == "input"
    
    FlattenUtils.traverse(data, (flatName, structPath, _) => {
      // structPath 是相对路径 (field.sub)，需要加上对象名
      val fullStructName = if (structObjName.isEmpty) structPath else s"$structObjName.$structPath"
      
      if (isInput) 
        verilog += s"  assign $fullStructName = $flatName;" // Input: Port -> Struct
      else 
        verilog += s"  assign $flatName = $fullStructName;" // Output: Struct -> Port
    })
  }
}