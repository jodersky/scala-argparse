import mill._, scalalib._, publish._, scalafmt._

class CmdrModule(val crossScalaVersion: String )
    extends CrossScalaModule
    with ScalafmtModule
    with PublishModule {

  def isDotty = crossScalaVersion.startsWith("0")
  def scalacOptions = if (!isDotty) Seq("-Ymacro-annotations", "-deprecation") else Seq("-deprecation")

  def ivyDeps = if (!isDotty) Agg(
    ivy"com.lihaoyi::os-lib:0.7.1",
    ivy"org.scala-lang:scala-reflect:${scalaVersion()}"
  ) else Agg(
    ivy"com.lihaoyi::os-lib:0.7.1"
  )
  object test extends Tests {
    def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.4")
    def testFrameworks = Seq("utest.runner.Framework")
  }
  def publishVersion = "0.3.0"
  def pomSettings = PomSettings(
    description = "cmdr",
    organization = "io.crashbox",
    url = "https://github.com/jodersky/cmdr",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("jodersky", "cmdr"),
    developers = Seq(
      Developer("jodersky", "Jakob Odersky", "https://githhub.com/jodersky")
    )
  )
  def artifactName = "cmdr"
}

object cmdr extends Cross[CmdrModule]("2.13.3", "0.27.0-RC1")

object examples extends Module {
  trait ExampleApp extends ScalaModule {
    def scalaVersion = cmdr("2.13.3").scalaVersion
    def scalacOptions = cmdr("2.13.3").scalacOptions
    def moduleDeps = Seq(cmdr("2.13.3"))
    def dist = T {
      val jar = assembly().path
      os.copy.over(jar, os.pwd / millSourcePath.last)
    }
  }
  object annotation extends ExampleApp
  object raw extends ExampleApp
  object readme extends ExampleApp
  object serverapp extends ExampleApp
  object flags extends ExampleApp
}
