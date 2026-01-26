package utility.dpic

import chisel3._
import chisel3.util._
import scala.collection.mutable.ListBuffer
import utility.dpic.CppStructGenerator.generateCppHeader

class DPICWrapper(bundle: DPICBundle, wrapperName: String) extends ExtModule {
  
  bundle.validateDirections() 
  
  val io: Bundle = IO(chisel3.reflect.DataMirror.internal.chiselTypeClone(bundle))
  override def desiredName: String = wrapperName

  private val inNode: Option[Data] = bundle.in
  private val outNode: Option[Data] = bundle.out
  private val enNode: Option[Bool] = bundle.en
  private val resetNode: Option[Bool] = bundle.reset

  require(inNode.isDefined || outNode.isDefined, 
    s"DPICBundle '${bundle.className}' must have at least 'in' or 'out' defined.")

  setInline(s"$desiredName.sv", generateVerilogModule())

  generateCppHeader(wrapperName, inNode, outNode, resetNode.isDefined, Some(s"${wrapperName}_dpic.h"))

  def generateVerilogModule(): String = {
    val verilog = ListBuffer.empty[String]

    val bundlesToCollect = Seq(inNode, outNode).flatten
    val collectedStructs = StructCollector.mergeUnique(
      bundlesToCollect.map(StructCollector.collectUniqueBundles).toSeq: _*
    )

    SVStructGenerator.generateSvStructs(null, verilog, collectedStructs) 

    inNode.foreach { d => 
      val func = s"${wrapperName}_put_in"
      val typeName = structName(d)
      verilog += s"import \"DPI-C\" function void $func(input $typeName bundle);"
    }
    
    outNode.foreach { d =>
      val func = s"${wrapperName}_get_out"
      val typeName = structName(d)
      verilog += s"import \"DPI-C\" function void $func(output $typeName bundle);"
    }

    val resetFunc = s"${wrapperName}_reset"
    if (resetNode.isDefined) {
      verilog += s"import \"DPI-C\" function void $resetFunc(input byte is_reset);\n"
    } else {
      verilog += "\n"
    }

    verilog += s"module $wrapperName ("
    SVStructGenerator.generatePortsList(bundle, verilog)
    verilog += ");\n"

    SVStructGenerator.generatePortDeclarations(bundle, verilog)
    
    inNode.foreach { d =>
      verilog += s"  ${structName(d)} in_struct;"
      SVStructGenerator.generateStructAssignment(d, verilog, "in_struct")
    }
    
    outNode.foreach { d =>
      verilog += s"  ${structName(d)} out_struct;"
      SVStructGenerator.generateStructAssignment(d, verilog, "out_struct")
    }

    verilog += "\n  always @(posedge clock) begin"

    if (resetNode.isDefined) {
      verilog += "    if (reset) begin"
      verilog += s"      $resetFunc(1);"
      verilog += "    end"
    }

    val prefix = if (resetNode.isDefined) "    else " else "    "
    
    if (enNode.isDefined) {
      verilog += s"${prefix}if (en) begin"
    } else {
      verilog += s"${prefix}begin"
    }

    if (resetNode.isDefined) {
        verilog += s"      $resetFunc(0);"
    }
    
    inNode.foreach { _ => verilog += s"      ${wrapperName}_put_in(in_struct);" }
    outNode.foreach { _ => verilog += s"      ${wrapperName}_get_out(out_struct);" }
    
    verilog += "    end"
    verilog += "  end"
    verilog += "endmodule"

    verilog.mkString("\n")
  }
  
  private def structName(d: Data) = TypeMapping.getStructName(d.asInstanceOf[Bundle])

  def getInputFuncName: Option[String] = 
    inNode.map(_ => s"${wrapperName}_put_in")

  def getOutputFuncName: Option[String] = 
    outNode.map(_ => s"${wrapperName}_get_out")

  def getResetFuncName: Option[String] = 
    if (resetNode.isDefined) Some(s"${wrapperName}_reset") else None
}