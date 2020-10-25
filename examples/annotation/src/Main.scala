object Main {

  @cmdr.main(
    "serverapp",
    "An example application which uses a synthetic reader derived from parameters.",
    "0.1.0"
  )
  def main(
      host: String = "localhost",
      @cmdr.alias("-p")
      @cmdr.help("The port to use")
      port: Int = 8080,
      flag: Boolean = false, // boolean args are parsed as flags
      path: java.nio.file.Path
  ): Unit = {
    println(s"$host:$port$path")
    println(s"flag: $flag")
  }

}
