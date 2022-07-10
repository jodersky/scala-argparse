def main(args: Array[String]): Unit =
  val parser = argparse.default.ArgumentParser()
  val verbosity = parser.param[Int](
    "--verbosity",
    default = 0,
    help = "level of verbosity"
  )
  val files = parser.repeatedParam[java.nio.file.Path](
    "files",
    help = "remove these files"
  )
  parser.parseOrExit(args)
  println(s"verbosity: ${verbosity.value}")
  for (file <- files.value) {
    println(s"if this were a real program, we would delete $file")
  }
