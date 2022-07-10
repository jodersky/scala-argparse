def main(args: Array[String]): Unit =
  val parser = argparse.default.ArgumentParser()
  val files = parser.repeatedParam[os.FilePath](
    "files",
    help = "remove these files"
  )
  parser.parseOrExit(args)
  for (file <- files.value) {
    println(s"if this were a real program, we would delete $file")
  }
