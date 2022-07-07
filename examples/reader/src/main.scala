case class Level(n: Int)

object custom extends argparse.core.Api {

  given Reader[Level] with {
    def read(str: String): Reader.Result[Level] = str match {
      case "DEBUG" => Reader.Success(Level(0))
      case "INFO" => Reader.Success(Level(1))
      case "WARN" => Reader.Success(Level(2))
      case "ERROR" => Reader.Success(Level(3))
      case other => Reader.Error(s"'$other' is not a valid level")
    }
    def typeName = "level"
  }

}

def main(args: Array[String]): Unit =
  val parser = custom.ArgumentParser() // notice how we use `custom` instead of `argparse.default`
  val level = parser.requiredParam[Level]("log-level")
  parser.parseOrExit(args)
  println(level.value.n)
