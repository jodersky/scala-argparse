package cmdr

import scala.collection.mutable

import Parser.ParamDef

object ArgParser {
  def apply(prog: String = "", description: String = "", version: String = "") =
    new ArgParser(prog, description, version)
}

/** A simple command line argument parser.
  *
  * = Usage =
  *
  * 1. Define parameters with [[param]], [[requiredParam]], [[repeatedParam]]
  *    and [[command]]. Each of these methods gives back a handle to a future
  *    argument value.
  *
  * 2. Call `parse()` with actual arguments.
  *
  * 3. If parsing succeeds, the arguments will be available in the handles
  *     defined in step 1.
  *
  *    If parsing fails, error descriptions are printed and the program exits
  *    with 2. (This behaviour may be changed by subclassing and redefining the
  *    `check()` method).
  *
  * = Example =
  *
  * {{{
  * val parser = cmdr.ArgumentParser("appname", "0.1.0") val p1 =
  * parser.param[String]("--this-is-a-named-param", default = "default value")
  * val p2 = parser.param[Int]("positional-param", default = 2)
  * parser.parse(Seq("--this-is-a-named-param=other", 5)) println(p1())
  * println(p2())
  * }}}
  *
  * @param prog the name of this command (only used in the default help message)
  * @param description a short description of this command. Used in help
  * messages.
  * @param version the verion of this app. Used in `--version` request.
  */
class ArgParser(
    prog: String,
    description: String,
    version: String
) { self =>

  private var errors = 0
  protected def reportMissing(name: String): Unit = {
    System.err.println(s"missing argument: $name")
    errors += 1
  }
  protected def reportUnknown(name: String): Unit = {
    System.err.println(s"unknown argument: $name")
    errors += 1
  }
  protected def reportParseError(name: String, message: String): Unit = {
    System.err.println(s"error processing argument $name: $message")
    errors += 1
  }
  protected def reportUnknownCommand(actual: String, available: Seq[String]) = {
    System.err.println("unknown command: " + _command())
    System.err.println("expected one of: " + available.mkString(", "))
    errors += 1
  }
  protected def showAndExit(msg: String): Unit = {
    System.err.println(msg)
    sys.exit(0)
  }

  // this should abort if any errors were encountered
  protected def check(): Boolean = {
    if (errors > 0) {
      System.err.println(s"try '$prog --help' for more information")
      sys.exit(2)
    } else {
      true
    }
  }
  protected def env: Map[String, String] = sys.env

  // used for generating help message
  private case class ParamInfo(
      isNamed: Boolean,
      names: Seq[String],
      isFlag: Boolean,
      repeats: Boolean,
      env: Option[String],
      description: String
  )
  private case class CommandInfo(name: String, action: Seq[String] => Unit)

  private val paramDefs = mutable.ListBuffer.empty[ParamDef]
  private val paramInfos = mutable.ListBuffer.empty[ParamInfo]
  private var commandInfos = mutable.ListBuffer.empty[CommandInfo]

  paramDefs += ParamDef(
    Seq("--help"),
    (_, _) => showAndExit(help()),
    missing = () => (),
    isFlag = true,
    repeatPositional = false,
    absorbRemaining = false
  )
  paramInfos += ParamInfo(
    true,
    Seq("--help"),
    true,
    false,
    None,
    "Show this message and exit"
  )

  paramDefs += ParamDef(
    Seq("--version"),
    (_, _) => showAndExit(version),
    missing = () => (),
    isFlag = true,
    repeatPositional = false,
    absorbRemaining = false
  )
  paramInfos += ParamInfo(
    true,
    Seq("--version"),
    true,
    false,
    None,
    "Show the version and exit"
  )

  def help(): String = {
    val (named0, positional) = paramInfos.span(_.isNamed)
    val named = named0.sortBy(_.names.head)

    val b = new StringBuilder
    b ++= s"Usage: $prog "
    if (!named.isEmpty) {
      b ++= "[OPTIONS] "
    }
    for (param <- positional) {
      b ++= s"<${param.names.head}>"
      if (param.repeats) b ++= "..."
      b ++= " "
    }
    b ++= "\n\n"
    b ++= description
    b ++= "\n"

    if (!commandInfos.isEmpty) {
      b ++= "\nCommand:\n"
      for (cmd <- commandInfos) {
        b ++= s"  ${cmd.name}\n"
      }
    }

    // Note that not necessarily all named parameters must be optional. However
    // since that is usually the case, this is what the default help message
    // assumes.
    if (!named.isEmpty) {
      b ++= "\nOptions:\n"
    }
    for (param <- named) {
      val names = if (param.isFlag) {
        param.names.mkString(", ")
      } else {
        param.names.map(_ + "=").mkString(", ")
      }
      b ++= s"\n  $names"
      if (param.repeats) b ++= "..."
      b ++= "\n"
      b ++= s"      ${param.description}\n"
      //b ++= f"  $names%-20s ${param.description}%-50s\n"
    }

    val envVars = named.filter(_.env.isDefined)
    if (!envVars.isEmpty) {
      b ++= "\nEnvironment:\n"
      for (param <- envVars) {
        b ++= f"  ${param.env.get}%-20s ${param.names.head}"
      }
    }

    b.result()
  }

  private class Completable[A](name: String) extends Arg[A] {
    var isComplete = false
    var _value: A = _
    def apply(): A =
      if (!isComplete) {
        throw new NoSuchElementException(
          s"This argument '$name' is not yet available. Make sure to call ArgumentParser#parse(args) before accessing this value."
        )
      } else {
        _value
      }
  }

  private def singleParam[A](
      name: String,
      default: => Option[A],
      env: Option[String],
      aliases: Seq[String],
      help: String,
      flag: Boolean,
      absorbRemaining: Boolean
  )(implicit reader: Reader[A]): Arg[A] = {
    val completable = new Completable[A](name)

    def read(name: String, strValue: String): Unit = {
      reader.read(strValue) match {
        case Left(message) => reportParseError(name, message)
        case Right(value) =>
          completable._value = value
          completable.isComplete = true
      }
    }

    def parseAndSet(name: String, valueOpt: Option[String]) = valueOpt match {
      case Some(v)      => read(name, v)
      case None if flag => read(name, "true")
      case None         => reportParseError(name, "argument expected")
    }

    val pdef = ParamDef(
      names = Seq(name) ++ aliases,
      parseAndSet = parseAndSet,
      missing = () => {
        val fromEnv = env.flatMap(self.env.get(_))

        fromEnv match {
          case Some(str) => parseAndSet(s"env ${env.get}", Some(str))
          case None if default.isDefined =>
            completable._value = default.get
            completable.isComplete = true
          case None => reportMissing(name)
        }
      },
      isFlag = flag,
      repeatPositional = false,
      absorbRemaining = absorbRemaining
    )
    paramDefs += pdef

    paramInfos += ParamInfo(
      pdef.isNamed,
      pdef.names,
      flag,
      false,
      env,
      help
    )

    completable
  }

  /** Define an optional parameter, using the given default value if it is not
    * supplied on the command line or by an environment variable.
    *
    * ErgoTip: always give named parameters a default value.
    *
    * ''Internal design note: [[param]] and [[requiredParam]] differ only in the
    * presence of the 'default' parameter. Ideally, they would be merged into one
    * single method, giving the 'default' parameter a default null value (as is
    * done for the other optional parameters, such as 'env' and 'help'). Unfortunately,
    * since 'default' is of type A where A may be a primitive type, it cannot
    * be assigned null. The usual solution would be to wrap it in an Option type,
    * but that leads to an ugly API. Hence the method is split into two.
    * See addParam() for the common denominator.''
    *
    * @tparam A The type to which an argument shall be converted.
    *
    * @param name The name of the parameter. A name starting with `-` indicates
    * a named parameter, whereas any other name indicates a positional parameter.
    * Prefer double-dash named params. I.e. prefer "--foo" over "-foo".
    *
    * @param default The default value to use in case no matching argument is provided.
    *
    * @param env The name of an environment variable from which to read the argument
    * in case it is not supplied on the command line. Set to 'null' to ignore.
    *
    * @param aliases Other names that may be used for this parameter. This is a
    * good place to define single-character aliases for frequently used
    * named parameters. Note that this has no effect for positional parameters.
    *
    * @param help A help message to display when the user types `--help`
    *
    * @param flag Set to true if the parameter should be treated as a flag. Flags
    * are named parameters that are treated specially by the parser:
    * - they never take arguments, unless the argument is embedded in the flag itself
    * - they are always assigned the string value "true" if found on the command line
    * Note that flags are intended to make it easy to pass boolean parameters; it is
    * quite rare that they are useful for non-boolean params.
    * The flag field has no effect on positional parameters.
    *
    * @return A handle to the parameter's future value, available once `parse(args)` has been called.
    */
  def param[A](
      name: String,
      default: => A,
      env: String = null,
      aliases: Seq[String] = Seq.empty,
      help: String = "",
      flag: Boolean = false,
      absorbRemaining: Boolean = false
  )(
      implicit reader: Reader[A]
  ): Arg[A] =
    singleParam(
      name,
      Some(default),
      Option(env),
      aliases,
      help,
      flag,
      absorbRemaining
    )

  /** Define a required parameter.
    *
    * This method is similar to [[param]], except that it does not accept a
    * default value. Instead, missing arguments for this parameter will cause
    * the parser to fail.
    *
    * ErgoTip: avoid named parameters that are required. Only require positional
    * parameters.
    *
    * @see param
    */
  def requiredParam[A](
      name: String,
      env: String = null,
      aliases: Seq[String] = Seq.empty,
      help: String = "",
      flag: Boolean = false,
      absorbRemaining: Boolean = false
  )(
      implicit reader: Reader[A]
  ): Arg[A] =
    singleParam(name, None, Option(env), aliases, help, flag, absorbRemaining)

  /** Define a parameter that may be repeated.
    *
    * Note that all named parameters may always be repeated, regardless if they
    * are defined as repeated or not. The difference is that for
    * non-repeat-defined parameters the last value is used, whereas
    * repeat-defined parameters accumulate values. (This is why
    * [[repeatedParam]] takes an `A` but gives back a `Seq[A]`, while other
    * params take `A` and give back `A`).
    *
    * E.g. consider the command line `--foo=1 --foo=2 --foo=3`
    *
    * In case foo is a regular named parameter, then, after parsing, the value
    * will be `3`. In case it is defined as a repeating parameter, its value
    * will be `Seq(1,2,3)`.
    *
    * Repeated positional parameters consume all remaining positional command
    * line arguments.
    */
  def repeatedParam[A](
      name: String,
      aliases: Seq[String] = Seq.empty,
      help: String = "",
      flag: Boolean = false
  )(implicit reader: Reader[A]): Arg[Seq[A]] = {
    var values = mutable.ArrayBuffer.empty[A]
    var isSet = false

    def read(name: String, strValue: String): Unit = {
      reader.read(strValue) match {
        case Left(message) => reportParseError(name, message)
        case Right(value) =>
          values += value
          isSet = true
      }
    }

    val pdef = ParamDef(
      names = Seq(name) ++ aliases,
      parseAndSet = (name, valueOpt) => {
        valueOpt match {
          case Some(v)      => read(name, v)
          case None if flag => read(name, "true")
          case None         => reportParseError(name, "argument expected")
        }
      },
      missing = () => (),
      flag,
      repeatPositional = true,
      absorbRemaining = false
    )
    paramDefs += pdef

    paramInfos += ParamInfo(
      pdef.isNamed,
      pdef.names,
      flag,
      true,
      None,
      help
    )

    new Arg[Seq[A]] {
      def apply(): Seq[A] = values.toList
    }
  }

  private var _command: Arg[String] = _
  private var _commandArgs: Arg[Seq[String]] = _

  /** Utility to define a sub command.
    *
    * Many modern command line apps actually consist of multiple nested
    * commands, each corresponding to the verb of an action, such as 'run' or
    * 'clone'. Typically, each sub command also has its own dedicated parameters
    * list.
    *
    * In cmdr, subcommands can easily be modelled by a positional parameter that
    * represents the command, followed by a repeated, all-absorbing parameter
    * which represents the command's arguments. However, since this pattern is
    * fairly common, this method is provided as a short-cut.
    *
    * @param name the name of the command
    * @param action a function called with the remaining arguments after this
    * command. Note that you may reference an Arg's value in the action.
    */
  def command(name: String, action: Seq[String] => Unit): Unit = {
    if (commandInfos.isEmpty) {
      _command = requiredParam[String]("command", absorbRemaining = true)
      _commandArgs = repeatedParam[String]("args")
    }
    commandInfos += CommandInfo(name, action)
  }

  /** Parse the given arguments with respect to the parameters defined by
    * [[param]], [[requiredParam]], [[repeatedParam]] and [[command]].
    *
    * In case no errors are encountered, the arguments will be populated in the
    * `Arg`s returned by the parameter definitions.
    *
    * In case errors are encountered, the default behaviour is to exit the
    * program.
    *
    * The classes of errors are:
    *
    * 1. An unknown argument is encountered. This can either be an unspecified
    *    named argument or an extranous positional argument.
    *
    * 2. A required argument is missing.
    *
    * 3. An argument cannot be parsed from its string value to its desired type.
    */
  def parse(args: Seq[String]): Unit = {
    Parser.parse(
      paramDefs.result(),
      args,
      reportUnknown
    )
    val ok = check()

    if (ok && !commandInfos.isEmpty) {
      commandInfos.find(_.name == _command()) match {
        case Some(cmd) =>
          cmd.action(_commandArgs())
        case None =>
          reportUnknownCommand(_command(), commandInfos.map(_.name).result())
          check()
      }
    }
  }
  def parse(args: Array[String]): Unit = parse(args.toSeq)

}
