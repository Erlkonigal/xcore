import os._
import mill._
import scalalib._
import scalafmt._

trait ChiselModule extends SbtModule with ScalafmtModule {
  def chiselVersion       = "7.2.0"
  def scalaTestVersion    = "3.2.19"
  override def scalaVersion = "2.13.16"
  override def scalacOptions = Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit",
    "-Ymacro-annotations"
  )
  override def ivyDeps = Agg(
    ivy"org.chipsalliance::chisel:$chiselVersion"
  )
  override def scalacPluginIvyDeps = Agg(
    ivy"org.chipsalliance:::chisel-plugin:$chiselVersion"
  )
}

object `package` extends RootModule with ChiselModule { m =>
  object sim extends ChiselModule {}

  object test extends SbtTests with TestModule.ScalaTest {
    override def ivyDeps = m.ivyDeps() ++ Agg(
      ivy"org.scalatest::scalatest::$scalaTestVersion"
    )
  }

  override def moduleDeps: Seq[JavaModule] = Seq(sim)
}
