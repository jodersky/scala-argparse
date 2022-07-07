def main(args: Array[String]): Unit = {
  val parser = argparse.default.ArgumentParser(description = "an example application")

  // a named parameter
  val host = parser.param[String](
    name = "--host",
    default = "localhost",
    help = "the name of the host"
  )

  // a named parameter which accepts only integers
  val port = parser.param[Int](
    name = "--port",
    default = 8080,
    aliases = Seq("-p"),
    env = "PORT",
    help = "some port"
  )

  // a named parameter which does not take an argument, aka a "flag"
  val secure = parser.param[Boolean](
    name = "--secure",
    default = false,
    flag = true
  )

  // a positional parameter which accepts only paths
  val path = parser.requiredParam[java.nio.file.Path](
    "path",
    help = "the path to use"
  )

  parser.parseOrExit(args)

  val scheme = if (secure.value) "https" else "http"
  val url = s"$scheme://${host.value}:${port.value}/${path.value}"
  println(url)
}
