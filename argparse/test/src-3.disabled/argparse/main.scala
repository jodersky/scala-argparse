package argparse


def main(args: Array[String]) = {
  val parser = argparse.ArgumentParser()

  parser.param[String]("--name1", default = "")
  parser.param[String]("--name2", default = "")
  parser.command("foo", a => {
    val p = argparse.ArgumentParser()
    p.command("yo", a => ())
    p.parseOrExit(a)
  })
  parser.command("bar", a => {
    val p = argparse.ArgumentParser()
    p.command("yo", a => ())
    p.parseOrExit(a)
  })
  parser.command("baz", a => {
    val p = argparse.ArgumentParser()
    p.command("yo", a => ())
    p.command("yo2", a => ())
    p.parseOrExit(a)
  })

  // StandaloneBashCompletion.all(
  //   System.out, "yooo", parser.paramInfos.toSeq, parser.commandInfos.toSeq
  // )
  parser.parseOrExit(args)

  // val cfg = parser.configParam("--config", makeConfigMap = argparse.Ini)
  // val p1 = parser.requiredParam[scala.concurrent.duration.Duration](
  //   "--param",
  //   config = "section1.foo"
  // )

  // parser.parseOrExit(args)
  // //println(cfg()("global_key"))

}
