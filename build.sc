import mill._, scalalib._, publish._, scalafmt._

val scala213 = "2.13.3"
val scala3 = "3.0.0-M2"

class CmdrModule(val crossScalaVersion: String)
    extends CrossScalaModule
    with ScalafmtModule
    with PublishModule {

  def isDotty = crossScalaVersion.startsWith("3")
  def scalacOptions = if (!isDotty) Seq("-Ymacro-annotations", "-deprecation") else Seq("-deprecation")

  def ivyDeps = if (!isDotty) Agg(
    ivy"com.lihaoyi::os-lib:0.7.1",
    ivy"org.scala-lang:scala-reflect:${scalaVersion()}"
  ) else Agg(
    ivy"com.lihaoyi::os-lib:0.7.1"
  )
  object test extends Tests {
    def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.5")
    def testFrameworks = Seq("utest.runner.Framework")
  }
  def publishVersion = "0.5.1"
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

object cmdr extends Cross[CmdrModule](scala213, scala3)

object examples extends Module {
  class ExampleApp(val crossScalaVersion: String) extends CrossScalaModule {
    def scalaVersion = cmdr(crossScalaVersion).scalaVersion
    def scalacOptions = cmdr(crossScalaVersion).scalacOptions
    def moduleDeps = Seq(cmdr(crossScalaVersion))
    def dist = T {
      val jar = assembly().path
      os.copy.over(jar, os.pwd / millSourcePath.last)
    }
    object test extends Tests {
      def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.5")
      def testFrameworks = Seq("utest.runner.Framework")
    }
  }
  object annotation extends Cross[ExampleApp](scala213)
  object readme extends Cross[ExampleApp](scala213, scala3)
  object commands extends Cross[ExampleApp](scala213, scala3)
}
