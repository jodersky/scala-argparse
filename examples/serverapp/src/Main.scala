object Main {

  @cmdr.main("serverapp", "An example application.",  "0.1.0")
  def main(
      host: String = "localhost",
      port: Int = 8080,
      path: java.nio.file.Path
  ): Unit = {
    println(s"$host:$port$path")
  }

}
