package argparse


def main(args: Array[String]) = {
  val parser = argparse.ArgParser()

  val cfg = parser.configParam("--config", makeConfigMap = argparse.Ini)
  val p1 = parser.requiredParam[scala.concurrent.duration.Duration](
    "--param",
    config = "section1.foo"
  )

  parser.parseOrExit(args)
  //println(cfg()("global_key"))

}
