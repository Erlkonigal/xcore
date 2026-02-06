import os._
import mill._
import scalalib._
import scalafmt._
import mill.scalalib.publish._
import coursier.maven.MavenRepository

import $file.common
import $file.`rocket-chip`.common
import $file.`rocket-chip`.dependencies.cde.common
import $file.`rocket-chip`.dependencies.diplomacy.common
import $file.`rocket-chip`.dependencies.hardfloat.common

object v {
  val scala = "2.13.18"
  val chiselVersion = "7.7.0"
  val chiselVersionIvy = 
      (ivy"org.chipsalliance::chisel:$chiselVersion", ivy"org.chipsalliance:::chisel-plugin:$chiselVersion")
  
  val mainargs = ivy"com.lihaoyi::mainargs:0.5.0"
  val json4sJackson = ivy"org.json4s::json4s-jackson:4.0.5"
  val scalaReflect = ivy"org.scala-lang:scala-reflect:${scala}"
  val sourcecode = ivy"com.lihaoyi::sourcecode:0.3.1"
  val sonatypesSnapshots = Seq(
    MavenRepository("https://s01.oss.sonatype.org/content/repositories/snapshots")
  )
}

trait Hardfloat
  extends $file.`rocket-chip`.dependencies.hardfloat.common.HardfloatModule {
    def scalaVersion: T[String] = T(v.scala)
    
    def chiselModule = None
    def chiselPluginJar: T[Option[PathRef]] = T(None)

    def chiselIvy: Option[Dep] = Some(v.chiselVersionIvy._1)
    def chiselPluginIvy: Option[Dep] = Some(v.chiselVersionIvy._2)

    override def millSourcePath: Path = super.millSourcePath / os.up / "rocket-chip" / "dependencies" / "hardfloat" / "hardfloat"
  }

object hardfloat extends Hardfloat

trait CDE
  extends $file.`rocket-chip`.dependencies.cde.common.CDEModule {
    def scalaVersion: T[String] = T(v.scala)
    override def millSourcePath: Path = super.millSourcePath / os.up / "rocket-chip" / "dependencies" / "cde" / "cde"
  }

object cde extends CDE

trait Diplomacy
  extends $file.`rocket-chip`.dependencies.diplomacy.common.DiplomacyModule {
    def scalaVersion: T[String] = T(v.scala)
    
    def chiselModule = None
    def chiselPluginJar: T[Option[PathRef]] = T(None)

    def chiselIvy: Option[Dep] = Some(v.chiselVersionIvy._1)
    def chiselPluginIvy: Option[Dep] = Some(v.chiselVersionIvy._2)

    def cdeModule = cde
    def sourcecodeIvy: Dep = v.sourcecode

    override def millSourcePath: Path = super.millSourcePath / os.up / "rocket-chip" / "dependencies" / "diplomacy" / "diplomacy"
  }

object diplomacy extends Diplomacy

trait Macros extends $file.`rocket-chip`.common.MacrosModule {
    def scalaVersion: T[String] = T(v.scala)
    def scalaReflectIvy: Dep = v.scalaReflect

    override def millSourcePath: Path = super.millSourcePath / os.up / "rocket-chip" / "macros"
}

object macros extends Macros

trait RocketChip
  extends $file.`rocket-chip`.common.RocketChipModule {
    def scalaVersion: T[String] = T(v.scala)
    
    def chiselModule = None
    def chiselPluginJar: T[Option[PathRef]] = T(None)

    def chiselIvy: Option[Dep] = Some(v.chiselVersionIvy._1)
    def chiselPluginIvy: Option[Dep] = Some(v.chiselVersionIvy._2)

    def cdeModule = cde
    def macrosModule = macros
    def hardfloatModule = hardfloat
    def diplomacyModule = diplomacy

    def mainargsIvy: Dep = v.mainargs
    def json4sJacksonIvy: Dep = v.json4sJackson

    override def millSourcePath: Path = super.millSourcePath / os.up / "rocket-chip"
  }
object rocketchip extends RocketChip

trait Utility
  extends $file.common.UtilityModule {
    def scalaVersion: T[String] = T(v.scala)

    def rocketChipModule = rocketchip

    def chiselModule = None
    def chiselPluginJar: T[Option[PathRef]] = T(None)

    def chiselIvy: Option[Dep] = Some(v.chiselVersionIvy._1)
    def chiselPluginIvy: Option[Dep] = Some(v.chiselVersionIvy._2)
  }

object utility extends Utility

trait XCore
  extends $file.common.XCoreModule {
    def scalaVersion: T[String] = T(v.scala)

    def rocketChipModule = rocketchip
    def utilityModule = utility
  
    def chiselModule = None
    def chiselPluginJar: T[Option[PathRef]] = T(None)

    def chiselIvy: Option[Dep] = Some(v.chiselVersionIvy._1)
    def chiselPluginIvy: Option[Dep] = Some(v.chiselVersionIvy._2)

    override def millSourcePath: Path = super.millSourcePath / os.up
  }

object xcore extends XCore