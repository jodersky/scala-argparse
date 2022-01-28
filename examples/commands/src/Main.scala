object Main {

  def main(args: Array[String]): Unit = {
    val parser = argparse.ArgParser(
      "gitz",
      "An example app that implements a tiny part of git's CLI, to illustrate nested commands."
    )

    val gitDir = parser.param[os.Path](
      "--git-dir",
      os.pwd / ".git"
    )

    parser.command("clone", args => clone(gitDir(), args), "clone a repository")
    parser.command("remote", args => remote(gitDir(), args), "manage remotes")

    parser.parseOrExit(args)
  }

  def clone(gitDir: => os.Path, args: Seq[String]): Unit = {
    val parser = argparse.ArgParser("gitz clone")

    val p1 = parser.param[Option[Int]]("--depth", None)
    val p2 = parser.requiredParam[String]("url")

    parser.parseOrExit(args)
    println("gitDir: " + gitDir)

    println(s"cloning ${p2()}")
    p1().foreach { depth => println("limiting depth to $depth") }
  }

  def remote(gitDir: => os.Path, args: Seq[String]): Unit = {
    val parser = argparse.ArgParser("gitz remote")

    parser.command("set-url", args => remoteSetUrl(gitDir, args))

    parser.parseOrExit(args)
  }

  def remoteSetUrl(gitDir: => os.Path, args: Seq[String]): Unit = {
    val parser = argparse.ArgParser("gitz remote set-url")

    val remote = parser.requiredParam[String]("remote")
    val url = parser.requiredParam[String]("url")

    parser.parseOrExit(args)
    println("gitDir: " + gitDir)

    println(s"setting remote ${remote()} to ${url()}")
  }

}
