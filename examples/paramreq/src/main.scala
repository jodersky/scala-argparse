def main(args: Array[String]): Unit =
  val parser = argparse.default.ArgumentParser()
  val echo = parser.requiredParam[String]("echo")
  parser.parseOrExit(args)
  print(echo.value)
