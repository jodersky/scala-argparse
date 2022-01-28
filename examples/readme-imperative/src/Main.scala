object Main {
  def main(args: Array[String]): Unit = {
    val parser = argparse.ArgParser(
      "readme",
      "An example application"
    )

    val host = parser.param[String](
      "--host",
      default = "localhost",
      help = "network host"
    )

    val port = parser.param[Int](
      "--port",
      default = 8080,
      aliases = Seq("-p"),
      env = "PORT",
      help = "some port"
    )

    val path = parser.requiredParam[java.nio.file.Path](
      "path",
      help = "the path to use"
    )

    parser.parseOrExit(args)
    println(s"${host()}:${port()}${path()}")
  }
}
