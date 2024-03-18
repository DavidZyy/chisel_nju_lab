// import mill._, scalalib._
// import coursier.maven.MavenRepository
// 
// // ./mill.sh myProject.runMain rv32e.top_main
// 
// object myProject extends SbtModule {
//   // Set the Scala version
//   def scalaVersion = "2.13.10"
//   // Uncomment the line below if you want to override the Scala version
//   // def scalaVersion = "2.12.18"
// 
//   // Set Scala compiler options
//   def scalacOptions = Seq(
//     "-feature",
//     "-language:reflectiveCalls"
//   )
// 
//   // Chisel 3.5
//   def ivyDeps = Agg(
//     ivy"edu.berkeley.cs:::chisel3-plugin:3.6.0", // three ":"
//     ivy"edu.berkeley.cs::chisel3:3.6.0",
//     ivy"edu.berkeley.cs::chiseltest:0.5.4"
//   )
//   override def millSourcePath = os.pwd // do not forget this line !!!!!!
//   // override def scalacPluginIvyDeps = Agg(
//   //   ivy"org.chipsalliance:::chisel-plugin:5.1.0",
//   // )
// }

// import Mill dependency
import mill._
import mill.define.Sources
import mill.modules.Util
import mill.scalalib.TestModule.ScalaTest
import scalalib._
// support BSP
import mill.bsp._

// input build.sc from each repositories.
import $file.`rocket-chip`.common
import $file.`rocket-chip`.cde.common
import $file.`rocket-chip`.hardfloat.build

object ivys {
  val sv = "2.13.12"
}

object myProject extends SbtModule { m =>
  override def millSourcePath = os.pwd
  override def scalaVersion = ivys.sv
  override def scalacOptions = Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit",
  )
  override def ivyDeps = Agg(
    ivy"org.chipsalliance::chisel:5.1.0",
  )
  override def scalacPluginIvyDeps = Agg(
    ivy"org.chipsalliance:::chisel-plugin:5.1.0",
  )
  object test extends SbtModuleTests with TestModule.ScalaTest {
    override def ivyDeps = m.ivyDeps() ++ Agg(
      ivy"edu.berkeley.cs::chiseltest:5.0.2"
    )
  }



  

  // add dependencies, ref xiangshan's build.sc

//   val defaultScalaVersion = ivys.sv
//
//   def defaultVersions = Map(
//     "chisel"        -> ivy"org.chipsalliance::chisel:5.1.0",
//     "chisel-plugin" -> ivy"org.chipsalliance:::chisel-plugin:5.1.0",
//     "chiseltest"    -> ivy"edu.berkeley.cs::chiseltest:5.0.2"
//   )
//
//   trait HasChisel extends SbtModule {
//   def chiselModule: Option[ScalaModule] = None
//
//   def chiselPluginJar: T[Option[PathRef]] = None
//
//   def chiselIvy: Option[Dep] = Some(defaultVersions("chisel"))
//
//   def chiselPluginIvy: Option[Dep] = Some(defaultVersions("chisel-plugin"))
//
//   override def scalaVersion = defaultScalaVersion
//
//   override def scalacOptions = super.scalacOptions() ++
//     Agg("-language:reflectiveCalls", "-Ymacro-annotations", "-Ytasty-reader")
//
//   override def ivyDeps = super.ivyDeps() ++ Agg(chiselIvy.get)
//
//   override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(chiselPluginIvy.get)
//   }
//
//
//   object rocketchip extends RocketChip
//
//
// trait RocketChip
//   extends millbuild.`rocket-chip`.common.RocketChipModule
//     with HasChisel {
//   def scalaVersion: T[String] = T(defaultScalaVersion)
//
//   override def millSourcePath = os.pwd / "rocket-chip"
//
//   def macrosModule = macros
//
//   // def hardfloatModule = hardfloat("chisel")
//   def hardfloatModule = hardfloat
//
//   def cdeModule = cde
//
//   def mainargsIvy = ivy"com.lihaoyi::mainargs:0.5.4"
//
//   def json4sJacksonIvy = ivy"org.json4s::json4s-jackson:4.0.6"
//
//   object macros extends Macros
//
//   trait Macros
//     extends millbuild.`rocket-chip`.common.MacrosModule
//       with SbtModule {
//
//     def scalaVersion: T[String] = T(defaultScalaVersion)
//
//     def scalaReflectIvy = ivy"org.scala-lang:scala-reflect:${defaultScalaVersion}"
//   }
//
//   // object hardfloat extends Cross[Hardfloat]("chisel")
//   object hardfloat extends Hardfloat
//
//   trait Hardfloat
//     extends millbuild.`rocket-chip`.hardfloat.common.HardfloatModule with HasChisel {
//
//     def scalaVersion: T[String] = T(defaultScalaVersion)
//
//     override def millSourcePath = os.pwd / "rocket-chip" / "hardfloat" / "hardfloat"
//
//   }
//
//   object cde extends CDE
//
//   trait CDE extends millbuild.`rocket-chip`.cde.common.CDEModule with ScalaModule {
//
//     def scalaVersion: T[String] = T(defaultScalaVersion)
//
//     override def millSourcePath = os.pwd / "rocket-chip" / "cde" / "cde"
//   }
// }
//
//
//   override def moduleDeps = super.moduleDeps ++ Seq(
//     rocketchip
//   )
}