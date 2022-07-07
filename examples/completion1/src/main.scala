def main(args: Array[String]): Unit =
  val parser = argparse.default.ArgumentParser()
  parser.param[os.Path]("--foo", os.pwd)
  parser.param[os.Path]("--bar", os.pwd)
  parser.param[os.Path](
    "--baz",
    os.pwd,
    standaloneCompleter = argparse.BashCompleter.Fixed(Set("a", "b"))
  )
  parser.parseOrExit(args)
