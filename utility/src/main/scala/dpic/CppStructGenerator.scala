package utility.dpic

import chisel3._
import scala.collection.mutable.ListBuffer

object CppStructGenerator {
  def generateCppHeader(baseName: String, in: Option[Data], out: Option[Data], hasReset: Boolean, headerName: Option[String] = None): Unit = {
    val fileName = headerName.getOrElse(s"$baseName.h")
    val cpp = ListBuffer.empty[String]
    
    val allBundles = StructCollector.mergeUnique(
      Seq(in, out).flatten.map(StructCollector.collectUniqueBundles).toSeq: _*
    )

    // --- Header Guard ---
    val guard = s"DPIC_${baseName.toUpperCase}_H"
    cpp += s"#ifndef $guard\n#define $guard\n"
    cpp += "#include <cstdint>\n#include <cstring>\n#include <iostream>\n"
    cpp += """
    |#ifdef __cplusplus
    |extern "C" {
    |#endif
    |""".stripMargin

    // --- Struct Definitions ---
    allBundles.foreach { case (name, bundle) =>
      cpp += s"struct __attribute__((packed)) $name {"
      bundle.elements.toSeq.reverse.foreach { case (fieldName, field) =>
        cpp += s"  ${getFieldDecl(field, fieldName, allBundles)}"
      }
      cpp += "};"
    }
    cpp += ""

    // --- DPI-C Functions ---
    in.foreach { d =>
      val structName = TypeMapping.getStructName(d.asInstanceOf[Bundle])
      val funcName = s"${baseName}_put_in" 
      cpp += s"void $funcName($structName in_data);"
    }

    out.foreach { d =>
      val structName = TypeMapping.getStructName(d.asInstanceOf[Bundle])
      val funcName = s"${baseName}_get_out"
      cpp += s"void $funcName($structName *out_data);"
    }

    if (hasReset) {
      cpp += s"void ${baseName}_reset(uint8_t is_reset);"
    }

    cpp += """
    |#ifdef __cplusplus
    |}
    |#endif
    |""".stripMargin

    cpp += s"\n#endif // $guard"
    
    GenController.addSource(fileName, cpp.toSeq)
  }

  private def getFieldDecl(field: Data, name: String, collected: Seq[(String, Bundle)]): String = {
    field match {
      case b: Bundle =>
        val typeName = StructCollector.lookupName(b, collected)
        s"$typeName $name;"
      case vec: Vec[_] =>
        val (elem, dims) = TypeMapping.getVecDimensions(vec)
        val dimStr = dims.map(d => s"[$d]").mkString
        elem match {
          case eb: Bundle =>
            val typeName = StructCollector.lookupName(eb, collected)
            s"$typeName $name$dimStr;"
          case _ =>
            s"${TypeMapping.getCppType(elem)} $name$dimStr;"
        }
      case _ =>
        s"${TypeMapping.getCppType(field)} $name;"
    }
  }
}