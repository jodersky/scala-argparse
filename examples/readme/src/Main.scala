object Main {
  def main(args: Array[String]): Unit = {
    val parser = cmdr.ArgumentParser()

    val host = parser.param[String](
      "--host",
      default = "localhost"
    )

    val port = parser.param[Int](
      "--port",
      default = 8080,
      aliases = Seq("-p"),
      env = "PORT"
    )

    val path = parser.requiredParam[java.nio.file.Path](
      "path"
    )
    parser.parse(args)
    println(s"${host()}:${port()}${path()}")
  }
}
