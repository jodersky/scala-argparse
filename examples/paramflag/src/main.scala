def main(args: Array[String]): Unit =
  val parser = argparse.default.ArgumentParser()
  val verbose = parser.param[Boolean](
    "--verbose",
    default = false,
    help = "use verbose output",
    flag = true
  )
  parser.parseOrExit(args)
  println(verbose.value)
