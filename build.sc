import mill._, scalalib._, scalanativelib._, publish._, scalafmt._

val scala213 = "2.13.6"
val scala3 = "3.0.2"
// Note: this is temporary. Once Scala 3.1.2 is released, we fall back to using
// only the latest Scala 3 version and the -Yrelease flag to target the oldest
// possible version.
val scala31 = "3.1.1"
val scalaNative = "0.4.4"
val dottyCustomVersion = Option(sys.props("dottyVersion"))

def gitVersion = T.input {
  os.proc("git", "describe", "--dirty=-SNAPSHOT").call(check = false).out.string.trim
}

trait Utest extends ScalaModule with TestModule {
  def ivyDeps = Agg(
    ivy"com.lihaoyi::utest::0.7.11",
    ivy"com.lihaoyi::upickle:1.5.0"
  )
  def testFramework = "utest.runner.Framework"
}

trait Publish extends PublishModule {
  def publishVersion = gitVersion()
  def pomSettings = PomSettings(
    description = "argparse",
    organization = "io.crashbox",
    url = "https://github.com/jodersky/scala-argparse",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("jodersky", "scala-argparse"),
    developers = Seq(
      Developer("jodersky", "Jakob Odersky", "https://github.com/jodersky")
    )
  )
}

object argparse extends Module {

  trait ArgParseModule
    extends CrossScalaModule
    with ScalafmtModule
    with Publish {

    def scalacOptions = Seq("-deprecation", "-release", "8")
    def ivyDeps = Agg(ivy"com.lihaoyi::os-lib::0.8.1")
    def artifactName = "argparse"
  }

  class JvmModule(val crossScalaVersion: String) extends ArgParseModule {
    def millSourcePath = super.millSourcePath / os.up
    def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / "src-jvm")))
    object test extends Tests with Utest
  }
  object jvm extends Cross[JvmModule]((Seq(scala213, scala3) ++ dottyCustomVersion):_*)

  class NativeModule(val crossScalaVersion: String, val crossScalaNativeVersion: String)
      extends ArgParseModule
      with ScalaNativeModule {
    def scalaNativeVersion = crossScalaNativeVersion
    def millSourcePath = super.millSourcePath / os.up / os.up
    def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / "src-native")))
    object test extends Tests with Utest
  }
  object native extends Cross[NativeModule]((scala213, scalaNative), (scala31, scalaNative))

}

object ini extends Module {

  trait IniModule
    extends CrossScalaModule
    with ScalafmtModule
    with Publish {

    def scalacOptions = Seq("-deprecation", "-release", "8")
    def ivyDeps = Agg(
      ivy"com.lihaoyi::os-lib::0.8.1"
    )
    def artifactName = "argparse-ini"
  }

  class JvmModule(val crossScalaVersion: String) extends IniModule {
    def millSourcePath = super.millSourcePath / os.up
    def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / "src-jvm")))
    object test extends Tests with Utest
  }
  object jvm extends Cross[JvmModule]((Seq(scala213, scala3) ++ dottyCustomVersion):_*)

  class NativeModule(val crossScalaVersion: String, val crossScalaNativeVersion: String)
      extends IniModule
      with ScalaNativeModule {
    def scalaNativeVersion = crossScalaNativeVersion
    def millSourcePath = super.millSourcePath / os.up / os.up
    def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / "src-native")))
  }
  object native extends Cross[NativeModule]((scala213, scalaNative), (scala31, scalaNative))

}

object examples extends Module {
  class ExampleApp(val crossScalaVersion: String) extends CrossScalaModule {
    def scalaVersion = argparse.jvm(crossScalaVersion).scalaVersion
    def scalacOptions = argparse.jvm(crossScalaVersion).scalacOptions
    def moduleDeps = Seq(argparse.jvm(crossScalaVersion))
    def dist = T {
      val jar = assembly().path
      os.copy.over(jar, os.pwd / millSourcePath.last)
    }
    object test extends Tests with Utest
  }
  object `readme-imperative` extends Cross[ExampleApp](scala213, scala3)
  object `readme-declarative` extends Cross[ExampleApp](scala3)
  object commands extends Cross[ExampleApp](scala213, scala3)
}
