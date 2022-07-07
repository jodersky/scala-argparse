def main(args: Array[String]): Unit =
  val parser = argparse.default.ArgumentParser()
  val echo: argparse.Argument[String] = parser.requiredParam[String](
    "echo",
    help = "echo the string you use here"
  )
  parser.parseOrExit(args)
  print(echo.value)
