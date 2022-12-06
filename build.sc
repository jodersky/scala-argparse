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
    object test extends Tests with Utest {
      def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / s"src-${crossScalaVersion.head}")))
    }
  }
  object jvm extends Cross[JvmModule]((Seq(scala213, scala3) ++ dottyCustomVersion):_*)

  class NativeModule(val crossScalaVersion: String, val crossScalaNativeVersion: String)
      extends ArgParseModule
      with ScalaNativeModule {
    def scalaNativeVersion = crossScalaNativeVersion
    def millSourcePath = super.millSourcePath / os.up / os.up
    def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / "src-native")))
    object test extends Tests with Utest {
      def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / s"src-${crossScalaVersion.head}")))
    }
  }
  object native extends Cross[NativeModule]((scala213, scalaNative), (scala31, scalaNative))

}

// experimental, typed configuration library
object configparse extends Module {

  object core extends Module {
    trait CoreConfigParseModule
      extends ScalaModule
      with ScalafmtModule
      with Publish {
      def scalaVersion = scala31 // this module is only available for Scala 3
      def scalacOptions = Seq("-deprecation", "-release", "8")
      def ivyDeps = Agg(ivy"com.lihaoyi::os-lib::0.8.1")
      def millSourcePath = super.millSourcePath / os.up
      def artifactName = "configparse-core"
    }
    object jvm extends CoreConfigParseModule {
      def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / "src-jvm")))
      object test extends Tests with Utest
    }
    object native extends CoreConfigParseModule with ScalaNativeModule {
      def scalaNativeVersion = scalaNative
      def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / "src-native")))
      object test extends Tests with Utest
    }
  }

  trait ConfigParseModule
    extends ScalaModule
    with ScalafmtModule
    with Publish {
    def scalaVersion = scala31 // this module is only available for Scala 3
    def scalacOptions = Seq("-deprecation", "-release", "8")
    def ivyDeps = Agg(
      ivy"com.lihaoyi::os-lib::0.8.1",
      ivy"io.crashbox::yamlesque::0.3.2"
    )
    def millSourcePath = super.millSourcePath / os.up
    def artifactName = "configparse"
  }
  object jvm extends ConfigParseModule {
    def moduleDeps = Seq(core.jvm, argparse.jvm(scala3))

    def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / "src-jvm")))
    object test extends Tests with Utest
  }
  object native extends ConfigParseModule with ScalaNativeModule {
    def moduleDeps = Seq(core.native, argparse.native(scala31, scalaNative))
    def scalaNativeVersion = scalaNative
    def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / "src-native")))
    object test extends Tests with Utest
  }
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
    // object test extends Tests with Utest
  }
  object native extends Cross[NativeModule]((scala213, scalaNative), (scala31, scalaNative))

}

object testutil extends ScalaNativeModule {
  def scalaVersion = scala31
  def scalaNativeVersion = scalaNative
  def scalacOptions = argparse.native(scala31, scalaNative).scalacOptions
  def ivyDeps = Agg(
    ivy"com.lihaoyi::utest::0.7.11",
    ivy"com.lihaoyi::os-lib::0.8.1"
  )
}

object examples extends Module {

  trait ExampleApp extends ScalaNativeModule { self =>
    def scalaVersion = scala31
    def scalaNativeVersion = scalaNative
    def scalacOptions = argparse.native(scala31, scalaNative).scalacOptions
    def moduleDeps = Seq(argparse.native(scala31, scalaNative))
    object test extends Tests with Utest {
      def moduleDeps = super.moduleDeps ++ Seq(testutil)
      def forkEnv = T {
        Map(
          "SNIPPET_FILE" -> (self.millSourcePath / "src" / "shell.txt").toString,
          "PATH" -> s"${self.nativeLink() / os.up}:${sys.env("PATH")}"
        )
      }
    }

    def nativeLink = T {
      os.Path(scalaNativeWorker().nativeLink(nativeConfig(), (T.dest / "app").toIO))
    }
  }

  object annotation extends ExampleApp
  object basic extends ExampleApp
  object completion1 extends ExampleApp
  object completion2 extends ExampleApp
  object help extends ExampleApp
  object paramdep extends ExampleApp
  object paramenv extends ExampleApp
  object paramflag extends ExampleApp
  object paramnamed extends ExampleApp
  object paramopt extends ExampleApp
  object paramrep extends ExampleApp
  object paramreq extends ExampleApp
  object paramreq2 extends ExampleApp
  object paramreq3 extends ExampleApp
  object paramshort extends ExampleApp
  object reader extends ExampleApp
  object readme extends ExampleApp

}

import $file.doctool.doctool
object docs extends doctool.DocsModule {
  def docVersion = gitVersion()
}
