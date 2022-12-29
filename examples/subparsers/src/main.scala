def main(args: Array[String]): Unit =
  val parser = argparse.default.ArgumentParser(description = "an example application")

  val global = parser.param[String]("--global", "unset")

  val getter = parser.subparser("get", "get a value")
  val getterKey = getter.requiredParam[String]("key")
  getter.action{
    println(s"global: ${global.value}")
    println(s"getting key ${getterKey.value}")
  }

  val setter = parser.subparser("set", "set a value")
  val setterKey = setter.requiredParam[String]("key")
  val setterValue = setter.requiredParam[String]("value")
  setter.action{
    println(s"global: ${global.value}")
    println(s"setting key ${setterKey.value} to ${setterValue.value}")
  }

  val nested = parser.subparser("nested", "another command")
  nested.subparser("inner1")
  nested.subparser("inner2")

  parser.parseOrExit(args)
