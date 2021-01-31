object Main {
  @cmdr.main(name = "readme", doc = "An example application")
  def main(
      @cmdr.arg(doc = "network host")
      host: String = "localhost",
      @cmdr.arg(doc = "some port", aliases = Seq("-p"), env = "PORT")
      port: Int = 8080,
      @cmdr.arg(doc = "the path to use")
      path: java.nio.file.Path
  ) = {
    println(s"$host:$port$path")
  }

  def main(args: Array[String]): Unit = cmdr.parseOrExit(args)
}
