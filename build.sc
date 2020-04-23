import mill._, scalalib._, scalafmt._

object cmdr extends ScalaModule with ScalafmtModule {
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

}

object examples extends Module {
  trait ExampleApp extends ScalaModule {
    def scalaVersion = cmdr.scalaVersion
    def scalacOptions = cmdr.scalacOptions
    def moduleDeps = Seq(cmdr)
  }
  object annotation extends ExampleApp
  object raw extends ExampleApp
}
