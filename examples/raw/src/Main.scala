object Main {

  // this shows how to manually use the argparser API
  def main(args: Array[String]) = {
    val parser = cmdr.ArgumentParser("test", "Serve pages from a given path", "0.0.1")

    val host = parser.param("--host", "localhost", help = "Interface to listen on")
    val port = parser.param("--port", 2222, env = "EXAMPLE_PORT", aliases=Seq("-p"), help = "Port to listen on")
    val path = parser.requiredParam[os.SubPath]("path")
    parser.parse(args)
    println(host())
    println(port())
    println(path())
  }

}
