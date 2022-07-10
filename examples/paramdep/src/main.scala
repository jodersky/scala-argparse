def main(args: Array[String]): Unit =
  val parser = argparse.default.ArgumentParser()
  val dir = parser.param[os.Path]("-C", default = os.root)
  val file = parser.param[os.Path]("--file", default = dir.value / "file")
  parser.parseOrExit(args)
  println(file.value)
