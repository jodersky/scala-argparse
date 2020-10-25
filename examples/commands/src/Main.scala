object Main {

  def main(args: Array[String]): Unit = {
    val parser = cmdr.ArgParser(
      "git",
      "An example app that implements a tiny part of git's CLI, to illustrate neseted commands."
    )

    val gitDir = parser.param[os.Path](
      "--git-dir",
      os.pwd / ".git"
    )

    parser.command("clone", args => clone(gitDir())(args))
    parser.command("remote", args => remote(gitDir())(args))

    parser.parse(args)
  }

  def clone(gitDir: os.Path)(args: Seq[String]): Unit = {
    val parser = cmdr.ArgParser()

    val p1 = parser.param[Option[Int]]("--depth", None)
    val p2 = parser.requiredParam[String]("url")

    parser.parse(args)
    println("gitDir: " + gitDir)

    println(s"cloning ${p2()}")
    p1().foreach { depth => println("slimiting depth to $depth") }
  }

  def remote(gitDir: os.Path)(args: Seq[String]): Unit = {
    val parser = cmdr.ArgParser()

    parser.command("set-url", args => remoteSetUrl(gitDir)(args))

    parser.parse(args)
  }

  def remoteSetUrl(gitDir: os.Path)(args: Seq[String]): Unit = {
    val parser = cmdr.ArgParser()

    val remote = parser.requiredParam[String]("remote")
    val url = parser.requiredParam[String]("url")

    parser.parse(args)
    println("gitDir: " + gitDir)

    println(s"setting remote ${remote()} to ${url()}")
  }

}
