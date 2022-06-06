package argparse

class ParamInfo[A](
  val reader: Reader[A],
  val name: String,
  var env: Option[String] = None,
  var aliases: List[String] = Nil,
  var default: Option[() => A] = None,
  var isFlag: Boolean = false,
  var absorbRemaining: Boolean = false,
  var repeats: Boolean = false
) {

  var isSet: Boolean = false
  val values = collection.mutable.ListBuffer.empty[A]

  def paramDef(
    reportParseError: (String, String) => Unit,
    reportMissing: String => Unit,
    environment: Map[String, String]
  ): Parser.ParamDef = {
    if (repeats) paramDefRepeated(reportParseError, reportMissing, environment)
    else paramDefSingle(reportParseError, reportMissing, environment)
  }

  private def paramDefSingle(
    reportParseError: (String, String) => Unit,
    reportMissing: String => Unit,
    environment: Map[String, String]
  ): Parser.ParamDef = {

    def read(name: String, strValue: String): Unit = {
      reader.read(strValue) match {
        case Reader.Error(message) => reportParseError(name, message)
        case Reader.Success(value) =>
          values.clear()
          values += value
          isSet = true
      }
    }

    def parseAndSet(name: String, valueOpt: Option[String]) = {
      valueOpt match {
        case Some(v) => read(name, v)
        case None    => reportParseError(name, "argument expected")
      }
      Parser.Continue
    }

    Parser.ParamDef(
      names = Seq(name) ++ aliases,
      parseAndSet = parseAndSet,
      missing = () => {
        val fromEnv = env.flatMap(environment.get(_))

        fromEnv match {
          case Some(str) => parseAndSet(s"from env ${env.get}", Some(str))
          case None if default.isDefined =>
            values.clear()
            values += default.get()
            isSet = true
          case None => reportMissing(name)
        }
      },
      isFlag = isFlag,
      repeatPositional = false,
      absorbRemaining = absorbRemaining
    )
  }

  private def paramDefRepeated(
    reportParseError: (String, String) => Unit,
    reportMissing: String => Unit,
    environment: Map[String, String]
  ): Parser.ParamDef = {
    isSet = true

    def read(name: String, strValue: String): Unit = {
      reader.read(strValue) match {
        case Reader.Error(message) => reportParseError(name, message)
        case Reader.Success(value) =>
          values += value
      }
    }

    Parser.ParamDef(
      names = Seq(name) ++ aliases,
      parseAndSet = (name, valueOpt) => {
        valueOpt match {
          case Some(v)      => read(name, v)
          case None if isFlag => read(name, "true")
          case None         => reportParseError(name, "argument expected")
        }
        Parser.Continue
      },
      missing = () => (),
      isFlag = isFlag,
      repeatPositional = true,
      absorbRemaining = false
    )
  }

}


/** An argument accessor is a function that returns an argument, assuming that
  * parsing was successful.
  */
trait BaseArg[A] {
  protected def info: ParamInfo[A]

  def alias(name: String*): this.type = {
    info.aliases = name.toList ::: info.aliases
    this
  }
  def env(name: String): this.type = {
    info.env = Some(name)
    this
  }

  def default(value: => A): this.type= {
    info.default = Some(() => value)
    this
  }

  def flag(): this.type = {
    info.isFlag = true
    this
  }

  def repeated(): RepeatedArg[A] = {
    info.repeats = true
    new RepeatedArg[A](info)
  }

}

class Arg[A](protected val info: ParamInfo[A]) extends BaseArg[A] {
  def absorbRemaining() = {
    info.absorbRemaining = true
    this
  }

  def value: A = if (info.isSet) info.values.head else
    throw new NoSuchElementException(
      s"This argument '${info.name}' is not yet available. ArgumentParser#parse(args) must be called before accessing this value."
    )
}

class RepeatedArg[A](protected val info: ParamInfo[A]) extends BaseArg[A] {

  def value: Seq[A] = if (info.isSet) info.values.toSeq else
    throw new NoSuchElementException(
      s"This argument '${info.name}' is not yet available. ArgumentParser#parse(args) must be called before accessing this value."
    )
}

class CliBuilder(stderr: java.io.PrintStream = System.err, env: Map[String, String] = sys.env) {

  private var errors: Int = 0
  // called when an expected (i.e. required) parameter is missing
  protected def reportMissing(name: String): Unit = {
    stderr.println(s"missing argument: $name")
    errors += 1
  }

  // called when an undeclared parameter is encountered
  protected def reportUnknown(name: String): Unit = {
    stderr.println(s"unknown argument: $name")
    errors += 1
  }

  protected[argparse] def reportParseError(name: String, message: String): Unit = {
    stderr.println(s"error processing argument $name: $message")
    errors += 1
  }

  private val infos = collection.mutable.ListBuffer.empty[ParamInfo[_]]

  def param[A](name: String)(implicit reader: Reader[A]): Arg[A] = {
    val p = new ParamInfo[A](reader, name)
    infos += p
    new Arg(p)
  }

  def parseOrExit(args: Iterable[String]): Unit = {
    val pdefs = infos.map(_.paramDef(reportParseError, reportMissing, env))

    Parser.parse(pdefs, args, reportUnknown)

    if (errors > 0) sys.exit(1)
  }

}

object Main {
  def main(args: Array[String]): Unit = {
    val parser = new CliBuilder()

    val pwd = parser
      .param[os.Path]("-C")
      .alias("-a", "-b")
      .default(os.pwd)

    val f = parser
      .param[os.FilePath]("-f")
      .default(pwd.value / "foo")
      // .map{
      //   case abs: os.Path => abs
      //   case rel => pwd.value / rel
      // }

    parser.parseOrExit(args)

    println(f.value)
  }
}
