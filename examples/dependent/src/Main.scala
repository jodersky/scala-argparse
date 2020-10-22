object Main {

  def main(args: Array[String]): Unit = {
    val parser = cmdr.ArgParser()

    val n = parser.param[Int](
      "-n",
      42
    )

    val nPlusOne = parser.param[Int](
      "-m",
      n() + 1
    )

    parser.parse(args)

    println(n())
    println(nPlusOne())
  }

}

