object Main {

  @cmdr.main("flags", "0.1.0")
  def main(
      flagOne: Boolean = false
  ): Unit = {
    println(flagOne)
  }

}
