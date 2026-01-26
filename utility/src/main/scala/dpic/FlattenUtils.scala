package utility.dpic

import chisel3._
import scala.collection.mutable.ListBuffer

object FlattenUtils {
  // 定义回调函数类型：(扁平化名称, 结构化名称(用于SV struct访问), 数据节点) => Unit
  type LeafHandler = (String, String, Data) => Unit

  def traverse(data: Data, handler: LeafHandler): Unit = {
    traverseRecursive(data, "", "", handler)
  }

  private def traverseRecursive(data: Data, flatPrefix: String, structPrefix: String, handler: LeafHandler): Unit = {
    data match {
      case b: Bundle =>
        b.elements.foreach { case (name, field) =>
          val newFlat = if (flatPrefix.isEmpty) name else s"${flatPrefix}_$name"
          val newStruct = if (structPrefix.isEmpty) name else s"${structPrefix}.$name"
          traverseRecursive(field, newFlat, newStruct, handler)
        }
      case vec: Vec[_] =>
        val (elem, dims) = TypeMapping.getVecDimensions(vec)
        // 生成多维数组的所有索引组合
        generateIndices(dims).foreach { indices =>
          val idxSuffix = indices.map(i => s"_$i").mkString
          val structSuffix = indices.map(i => s"[$i]").mkString
          val newFlat = s"$flatPrefix$idxSuffix"
          val newStruct = s"$structPrefix$structSuffix"
          
          elem match {
            case nestedB: Bundle => traverseRecursive(nestedB, newFlat, newStruct, handler)
            case _ => handler(newFlat, newStruct, elem)
          }
        }
      case leaf =>
        handler(flatPrefix, structPrefix, leaf)
    }
  }

  // 生成维度索引组合，例如 dims=[2,2] -> [[0,0], [0,1], [1,0], [1,1]]
  private def generateIndices(dims: List[Int]): List[List[Int]] = {
    if (dims.isEmpty) List(List())
    else {
      val headDim = dims.head
      val tailResults = generateIndices(dims.tail)
      (0 until headDim).flatMap(i => tailResults.map(i :: _)).toList
    }
  }
}