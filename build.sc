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

object myProject extends SbtModule { m =>
  override def millSourcePath = os.pwd
  override def scalaVersion = "2.13.12"
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
}