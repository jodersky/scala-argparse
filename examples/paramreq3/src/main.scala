def main(args: Array[String]): Unit =
  val parser = argparse.default.ArgumentParser()
  val square = parser.requiredParam[Int](
    "square",
    help = "display a square of a given number"
  )
  parser.parseOrExit(args)
  print(square.value * square.value)
