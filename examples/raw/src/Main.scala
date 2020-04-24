object Main {

  // this shows how to manually use the argparser API
  def main(args: Array[String]) = {
    val parser = cmdr.ArgumentParser("test", "0.0.1", "Serve pages from a given path")

    val host = parser.param("--host", "localhost", help = "Interface to listen on")
    val port = parser.param("--port", 2222, env = "EXAMPLE_PORT", aliases=Seq("-p"), help = "Port to listen on")
    val path = parser.requiredParam[os.SubPath]("path")
    parser.parse(args)
    println(host.get)
    println(port.get)
    println(path.get)
  }

}
