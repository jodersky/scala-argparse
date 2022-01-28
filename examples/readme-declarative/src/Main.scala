object Main {
  @argparse.main(name = "readme", doc = "An example application")
  def main(
      @argparse.arg(doc = "network host")
      host: String = "localhost",
      @argparse.arg(doc = "some port", aliases = Seq("-p"), env = "PORT")
      port: Int = 8080,
      @argparse.arg(doc = "the path to use")
      path: java.nio.file.Path
  ) = {
    println(s"$host:$port$path")
  }

  def main(args: Array[String]): Unit = argparse.parseOrExit(args)
}
