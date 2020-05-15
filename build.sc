import mill._, scalalib._, publish._, scalafmt._

object cmdr extends ScalaModule with ScalafmtModule with PublishModule {
  def scalaVersion = "2.13.1"
  def scalacOptions = Seq("-Ymacro-annotations")

  def ivyDeps = Agg(
    ivy"com.lihaoyi::os-lib:0.6.3",
    ivy"org.scala-lang:scala-reflect:${scalaVersion()}"
  )
  object test extends Tests {
    def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.4")
    def testFrameworks = Seq("utest.runner.Framework")
  }
  def publishVersion = "0.2.1"
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
}

object examples extends Module {
  trait ExampleApp extends ScalaModule {
    def scalaVersion = cmdr.scalaVersion
    def scalacOptions = cmdr.scalacOptions
    def moduleDeps = Seq(cmdr)
    def dist = T {
      val jar = assembly().path
      os.copy.over(jar, os.pwd / millSourcePath.last)
    }
  }
  object annotation extends ExampleApp
  object raw extends ExampleApp
  object serverapp extends ExampleApp
  object flags extends ExampleApp
}
