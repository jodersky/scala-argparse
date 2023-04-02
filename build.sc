import mill._, scalalib._, scalanativelib._, publish._, scalafmt._

val scala213 = "2.13.10"
val scala3 = "3.2.2"
val scalaNative = "0.4.12"
val dottyCustomVersion = Option(sys.props("dottyVersion"))

def gitVersion = T.input {
  os.proc("git", "describe", "--dirty=-SNAPSHOT").call(check = false).out.string.trim
}

val VersionHeader = """## (\d.*)""".r
def releaseVersion = T.input {
  val lines = os.read.lines.stream(os.pwd / "CHANGELOG.md")
  val version = lines.collectFirst{
    case VersionHeader(version) => version
  }.getOrElse("<latest version>")
  os.write(T.dest / "version", version)
  version
}

trait Utest extends ScalaModule with TestModule {
  def ivyDeps = Agg(
    ivy"com.lihaoyi::utest::0.8.1",
    ivy"com.lihaoyi::upickle:3.0.0"
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
    def ivyDeps = Agg(ivy"com.lihaoyi::os-lib::0.9.1")
    def artifactName = "argparse"
  }

  class JvmModule(val crossScalaVersion: String) extends ArgParseModule { main =>
    def millSourcePath = super.millSourcePath / os.up
    def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / "src-jvm")))

    def superOptions = T{super.scalacOptions()}

    def scalacOptions = if (crossScalaVersion.startsWith("3")) {
      // it's enough to check macros for only one configuration
      superOptions() ++ Seq("-Xcheck-macros", "-Ycheck:all")
    } else superOptions()

    object test extends Tests with Utest {
      def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / s"src-${crossScalaVersion.head}")))
      def scalacOptions = main.superOptions()
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
  object native extends Cross[NativeModule]((scala213, scalaNative), (scala3, scalaNative))

  object sandbox extends ScalaModule {
    def moduleDeps = Seq(jvm(scala3))
    def scalaVersion = scala3
    def scalacOptions = Seq("-Xprint:inline")
  }

}

// experimental, typed configuration library
object configparse extends Module {

  object core extends Module {
    trait CoreConfigParseModule
      extends ScalaModule
      with ScalafmtModule
      with Publish {
      def scalaVersion = scala3 // this module is only available for Scala 3
      def scalacOptions = Seq("-deprecation", "-release", "8")
      def ivyDeps = Agg(ivy"com.lihaoyi::os-lib::0.9.1")
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
    def scalaVersion = scala3 // this module is only available for Scala 3
    def scalacOptions = Seq("-deprecation", "-release", "8")
    def ivyDeps = Agg(
      ivy"com.lihaoyi::os-lib::0.9.1",
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
    def moduleDeps = Seq(core.native, argparse.native(scala3, scalaNative))
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
      ivy"com.lihaoyi::os-lib::0.9.1"
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
  object native extends Cross[NativeModule]((scala213, scalaNative), (scala3, scalaNative))

}

object testutil extends ScalaNativeModule {
  def scalaVersion = scala3
  def scalaNativeVersion = scalaNative
  def scalacOptions = argparse.native(scala3, scalaNative).scalacOptions
  def ivyDeps = Agg(
    ivy"com.lihaoyi::utest::0.8.1",
    ivy"com.lihaoyi::os-lib::0.9.1"
  )
}

object examples extends Module {

  trait ExampleApp extends ScalaNativeModule { self =>
    def scalaVersion = scala3
    def scalaNativeVersion = scalaNative
    def scalacOptions = argparse.native(scala3, scalaNative).scalacOptions
    def moduleDeps = Seq(argparse.native(scala3, scalaNative))
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

  object `annotation-intro` extends ExampleApp
  object `annotation-mappings` extends ExampleApp
  object `annotation-types` extends ExampleApp
  object `annotation-commands` extends ExampleApp
  object `annotation-annot` extends ExampleApp
  object `annotation-output` extends ExampleApp
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
  object subparsers extends ExampleApp

}

object book extends Module {
  def book = T.input {
    val env = Map(
      "MDBOOK_BOOK__title" -> s"scala-argparse ${releaseVersion()}"
    )
    os.proc("mdbook", "build", "--dest-dir", T.dest, millSourcePath).call(env = env, stdout = os.Inherit)
    os.copy(argparse.jvm(scala3).docJar().path / os.up / "javadoc", T.dest / "javadoc")
    T.dest
  }
}
