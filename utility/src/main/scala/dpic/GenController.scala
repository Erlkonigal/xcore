package utility.dpic

import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets

object GenController {
  val generatedSources = scala.collection.mutable.ListBuffer.empty[(String, Seq[String])]

  def addSource(fileName: String, content: Seq[String]): Unit = {
    // deduplicating based on fileName
    if (!generatedSources.exists(_._1 == fileName)) {
      generatedSources += ((fileName, content))
    }
  }

  def writeAllSources(outputDir: String): Unit = {
    Files.createDirectories(Paths.get(outputDir))
    generatedSources.foreach { case (fileName, content) =>
      val path = Paths.get(outputDir, fileName)
      Files.write(path, content.mkString("\n").getBytes(StandardCharsets.UTF_8))
    }
  }
}