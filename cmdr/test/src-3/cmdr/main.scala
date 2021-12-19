package cmdr


def main(args: Array[String]) = {
  val parser = cmdr.ArgParser()

  val cfg = parser.configParam("--config", makeConfigMap = cmdr.Ini)
  val p1 = parser.requiredParam[scala.concurrent.duration.Duration](
    "--param",
    config = "section1.foo"
  )

  parser.parseOrExit(args)
  //println(cfg()("global_key"))

}
