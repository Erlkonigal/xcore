package utility.dpic

import chisel3._
import scala.collection.mutable

object StructCollector {
  // 返回一个列表，包含 (结构体名称, Bundle实例)。
  def collectUniqueBundles(root: Data): Seq[(String, Bundle)] = {
    val seen = mutable.ListBuffer.empty[(String, Bundle)]
    
    def process(d: Data): Unit = d match {
      case b: Bundle =>
        // 1. 先递归处理子元素（保证依赖顺序：子结构体先定义）
        b.elements.foreach { case (_, field) => process(field) }
        
        // 2. 检查是否已存在相同结构的 Bundle
        val baseName = TypeMapping.getStructName(b)
        // 注意：这里使用 structural 比较
        val existingIdx = seen.indexWhere { case (_, existing) => TypeMapping.isBundleSame(b, existing) }
        
        if (existingIdx == -1) {
          // 处理同名但结构不同的情况 (e.g. MyBundle_1, MyBundle_2)
          val nameConflictCount = seen.count { case (n, _) => n.startsWith(baseName) }
          val finalName = if (nameConflictCount == 0) baseName else s"${baseName}_${nameConflictCount}"
          seen += ((finalName, b))
        }
        
      case v: Vec[_] =>
        val (elem, _) = TypeMapping.getVecDimensions(v)
        process(elem)
      case _ =>
    }

    if (root != null) process(root)
    seen.toSeq // Scala 2.12: ListBuffer 可以直接转 Seq
  }

  // 查找给定 Bundle 在 collected 列表中的名称
  def lookupName(b: Bundle, collected: Seq[(String, Bundle)]): String = {
    collected.find { case (_, existing) => TypeMapping.isBundleSame(b, existing) }
      .map(_._1)
      .getOrElse(throw new Exception(s"Struct not found for bundle: $b"))
  }

  // --- 新增：Scala 2.12 兼容的去重合并方法 ---
  def mergeUnique(lists: Seq[(String, Bundle)]*): Seq[(String, Bundle)] = {
    val all = lists.flatten
    val seenNames = mutable.HashSet.empty[String]
    // 保留顺序的同时去重
    all.filter { case (name, _) =>
      if (seenNames.contains(name)) false
      else {
        seenNames += name
        true
      }
    }
  }
}