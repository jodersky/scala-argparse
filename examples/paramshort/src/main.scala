def main(args: Array[String]): Unit =
  val parser = argparse.default.ArgumentParser()
  val verbose = parser.param[Boolean](
    "--verbose",
    default = false,
    aliases = Seq("-v", "--talkative"),
    flag = true,
    help="use verbose output"
  )
  parser.parseOrExit(args)
  println(verbose.value)
