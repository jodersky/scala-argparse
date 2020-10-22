object Main {

  def main(args: Array[String]): Unit = {
    val parser = cmdr.ArgParser("subcommand", "run a command")

    val global = parser.param[Int](
      "--global",
      42
    )

    parser.command("cmd1", args => cmd1(global())(args))
    parser.command("cmd2", args => cmd2(global())(args))
    parser.command("cmd3", args => cmd3(global())(args))

    parser.parse(args)
  }

  def cmd1(global: Int)(args: Seq[String]): Unit = {
    val parser = cmdr.ArgParser()

    val p1 = parser.param[String]("--param1", "value1")
    val p2 = parser.requiredParam[String]("param")

    parser.parse(args)
    println("global: " + global)

    println("running cmd1")
    println(p1())
    println(p2())
  }

  def cmd2(global: Int)(args: Seq[String]): Unit = {
    val parser = cmdr.ArgParser()

    val p1 = parser.param[String]("--param1", "value1")
    val p2 = parser.requiredParam[String]("param")

    parser.parse(args)
    println("global: " + global)

    println("running cmd2")
    println(p1())
    println(p2())
  }

  def cmd3(global: Int)(args: Seq[String]): Unit = {
    val parser = cmdr.ArgParser()

    val p1 = parser.param[String]("--param1", "value1")

    parser.command("cmd4", {args => println(p1()); cmd4(args)})
    parser.parse(args)
  }

  def cmd4(args: Seq[String]): Unit = {
    val parser = cmdr.ArgParser()
    parser.parse(args)
    println("cmd4")
  }

}
