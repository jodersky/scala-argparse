def main(args: Array[String]): Unit =
  val parser = argparse.default.ArgumentParser()
  val answer = parser.param[Int](
    "answer",
    default = 42,
    help="display the answer to the universe"
  )
  parser.parseOrExit(args)
  print(answer.value)
