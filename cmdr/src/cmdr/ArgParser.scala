package cmdr

import scala.collection.mutable

import Parser.ParamDef

object ArgParser {
  def apply(
    prog: String = "",
    description: String = "",
    version: String = "",
    reporter: Reporter = new Reporter,
    env: Map[String, String] = sys.env
  ) = new ArgParser(prog, description, version, reporter, env)

  type Completer = String => Seq[String]
  val NoCompleter = (s: String) => Seq.empty

  /** User-friendly parameter information, used for generating help message */
  case class ParamInfo(
      isNamed: Boolean,
      names: Seq[String],
      isFlag: Boolean,
      repeats: Boolean,
      env: Option[String],
      description: String,
      completer: Completer
  )
  case class CommandInfo(
      name: String,
      action: Seq[String] => Unit,
      description: String
  )

  sealed trait Result
  case object Success extends Result
  case object Error extends Result
  case object EarlyExit extends Result

  private class EarlyExitException extends Exception

  class Reporter{
    def stdout: java.io.PrintStream = System.out
    def stderr: java.io.PrintStream = System.err

    private var errors = 0

    // called when an expected (i.e. required) parameter is missing
    def reportMissing(name: String): Unit = {
      stderr.println(s"missing argument: $name")
      errors += 1
    }

    // called when an undeclared parameter is encountered
    def reportUnknown(name: String): Unit = {
      stderr.println(s"unknown argument: $name")
      errors += 1
    }

    def reportParseError(name: String, message: String): Unit = {
      stderr.println(s"error processing argument $name: $message")
      errors += 1
    }

    def reportUnknownCommand(actual: String, available: Seq[String]) = {
      stderr.println("unknown command: " + actual)
      stderr.println("expected one of: " + available.mkString(", "))
      errors += 1
    }

    def hasErrors = errors > 0
  }

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
    val prog: String,
    val description: String,
    val version: String,
    val reporter: ArgParser.Reporter,
    val env: Map[String, String]
) extends SettingsParser { self =>
  import ArgParser._

  private val paramDefs = mutable.ListBuffer.empty[ParamDef]
  private val paramInfos = mutable.ListBuffer.empty[ParamInfo]
  private var commandInfos = mutable.ListBuffer.empty[CommandInfo]

  /** Low-level escape hatch for manually adding parameter definitions.
    *
    * See also [[param]], [[requiredParam]] and [[repeatedParam]] for the
    * high-level API.
    */
  def addParamDef(pdef: ParamDef): Unit = paramDefs += pdef

  /** Low-level escape hatch for manually adding parameter information.
    *
    * See also [[param]], [[requiredParam]] and [[repeatedParam]] for the
    * high-level API.
    */
  def addParamInfo(pinfo: ParamInfo): Unit = paramInfos += pinfo

  paramDefs += ParamDef(
    Seq("--help"),
    (_, _) => {
      reporter.stdout.println(help())
      throw new EarlyExitException
    },
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
    "show this message and exit",
    NoCompleter
  )

  // the --version flag is only relevant if a version has been specified
  if (version != "") {
    paramDefs += ParamDef(
      Seq("--version"),
      (_, _) => {
        reporter.stdout.println(version)
        throw new EarlyExitException
      },
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
      "show the version and exit",
      NoCompleter
    )
  }

  private def help(): String = {
    val (named0, positional) = paramInfos.partition(_.isNamed)
    val named = named0.sortBy(_.names.head)

    val b = new StringBuilder
    b ++= s"usage: $prog "
    if (!named.isEmpty) {
      b ++= "[options] "
    }
    for (param <- positional) {
      b ++= s"<${param.names.head}>"
      if (param.repeats) b ++= "..."
      b ++= " "
    }
    b ++= "\n"

    if (!description.isEmpty()) {
      b ++= "\n"
      b ++= description
      b ++= "\n\n"
    }

    if (!commandInfos.isEmpty) {
      b ++= "commands:\n"
      for (cmd <- commandInfos) {
        b ++= f" ${cmd.name}%-14s ${cmd.description}%-58s%n"
      }
    }

    val describedPos = positional.filter(!_.description.isEmpty)
    if (!describedPos.isEmpty && commandInfos.isEmpty) {
      b ++= "positional arguments:\n"
      for (param <- positional) {
        b ++= f" ${param.names.head}%-14s ${param.description}%-58s%n"
      }
    }

    // Note that not necessarily all named parameters must be optional. However
    // since that is usually the case, this is what the default help message
    // assumes.
    if (!named.isEmpty) {
      b ++= "named arguments:\n"
    }
    for (param <- named) {
      val names = if (param.isFlag) {
        param.names.mkString(", ")
      } else {
        param.names.map(_ + "=").mkString(", ")
      }
      b ++= f" $names%-14s ${param.description}%-58s%n"
    }

    val envVars = named.filter(_.env.isDefined)
    if (!envVars.isEmpty) {
      b ++= "environment variables:\n"
      for (param <- envVars) {
        b ++= f" ${param.env.get}%-14s ${param.names.head}%s%n"
      }
    }

    b.result()
  }

  def singleParam[A](
      name: String,
      default: Option[() => A],
      env: Option[String],
      aliases: Seq[String],
      help: String,
      flag: Boolean,
      absorbRemaining: Boolean,
      completer: Option[Completer]
  )(implicit reader: Reader[A]): () => A = {
    var setValue: Option[A] = None

    def read(name: String, strValue: String): Unit = {
      reader.read(strValue) match {
        case Reader.Error(message) => reporter.reportParseError(name, message)
        case Reader.Success(value) => setValue = Some(value)
      }
    }

    def parseAndSet(name: String, valueOpt: Option[String]) = valueOpt match {
      case Some(v) => read(name, v)
      case None    => reporter.reportParseError(name, "argument expected")
    }

    val pdef = ParamDef(
      names = Seq(name) ++ aliases,
      parseAndSet = parseAndSet,
      missing = () => {
        val fromEnv = env.flatMap(self.env.get(_))

        fromEnv match {
          case Some(str) => parseAndSet(s"env ${env.get}", Some(str))
          case None if default.isDefined =>
            setValue = Some(default.get())
          case None => reporter.reportMissing(name)
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
      help,
      completer.getOrElse(reader.completer)
    )

    () =>
      setValue.getOrElse(
        throw new NoSuchElementException(
          s"This argument '$name' is not yet available. ArgumentParser#parse(args) must be called before accessing this value."
        )
      )
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
    * @param absorbRemaining Indicates that any arguments encountered after this parameter
    * must be treated as positionals, even if they start with `-`. In other words, a
    * parameter marked with this has the same effect as the `--` separator. It can be
    * useful for implementing sub-commands. (Note however that this ArgParser has a
    * dedicated `command` method for such use cases)
    *
    * @param completer A bash snippet that is inserted in bash-completions, responsible for setting
    * completion options for this param. If omitted, the parameter type's (A) default completer
    * will be used. If present, this must be valid bash and should set COMPREPLY. The bash variable
    * "$cur" may be used in the snippet, and will contain the current word being completed for this
    * parameter.
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
      absorbRemaining: Boolean = false,
      completer: Completer = null
  )(
      implicit reader: Reader[A]
  ): () => A =
    singleParam(
      name,
      Some(() => default),
      Option(env),
      aliases,
      help,
      flag,
      absorbRemaining,
      Option(completer)
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
      absorbRemaining: Boolean = false,
      completer: Completer = null
  )(
      implicit reader: Reader[A]
  ): () => A =
    singleParam(
      name,
      None,
      Option(env),
      aliases,
      help,
      flag,
      absorbRemaining,
      Option(completer)
    )

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
      flag: Boolean = false,
      completer: Completer = null
  )(implicit reader: Reader[A]): () => Seq[A] = {
    var values = mutable.ArrayBuffer.empty[A]
    var isSet = false

    def read(name: String, strValue: String): Unit = {
      reader.read(strValue) match {
        case Reader.Error(message) => reporter.reportParseError(name, message)
        case Reader.Success(value) =>
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
          case None         => reporter.reportParseError(name, "argument expected")
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
      help,
      Option(completer).getOrElse(reader.completer)
    )

    () => values.toList
  }

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
    * fairly common, this method is provided as a shortcut.
    *
    * @param name the name of the command
    * @param action a function called with the remaining arguments after this
    * command. Note that you may reference an Arg's value in the action.
    */
  def command(
      name: String,
      action: Seq[String] => Unit,
      description: String = ""
  ): Unit = {
    commandInfos += CommandInfo(name, action, description)
  }

  /** Parse the given arguments with respect to the parameters defined by
    * [[param]], [[requiredParam]], [[repeatedParam]] and [[command]].
    */
  def parseResult(args0: Iterable[String]): Result = {
    val args = args0.toSeq
    var _command: () => String = null
    var _commandArgs: () => Seq[String] = null

    if (!commandInfos.isEmpty) {
      val commands = commandInfos.map(_.name)
      _command = requiredParam[String](
        "command",
        absorbRemaining = true,
        completer = prefix => commands.filter(_.startsWith(prefix)).toList
      )
      _commandArgs = repeatedParam[String](
        "args"
      )
    }

    if (BashCompletion.completeOrFalse(paramInfos.toList, commandInfos.toList, env, args, reporter.stdout)) {
      return EarlyExit
    }

    try {
      Parser.parse(
        paramDefs.result(),
        args,
        reporter.reportUnknown
      )
    } catch {
      case _: EarlyExitException => return EarlyExit
    }

    if (reporter.hasErrors) return Error

    if (!commandInfos.isEmpty) {
      commandInfos.find(_.name == _command()) match {
        case Some(cmd) =>
          cmd.action(_commandArgs())
        case None =>
          reporter.reportUnknownCommand(_command(), commandInfos.map(_.name).result())
          return Error
      }
    }

    Success
  }

  /** Parse the given arguments with respect to the parameters defined by
    * [[param]], [[requiredParam]], [[repeatedParam]] and [[command]].
    *
    * In case no errors are encountered, the arguments will be populated in the
    * functions returned by the parameter definitions.
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
    *
    * @see parseResult for a version of this function which does not exit
    */
  def parseOrExit(args: Iterable[String]): Unit = parseResult(args) match {
    case Success => ()
    case EarlyExit => sys.exit(0)
    case Error =>
      reporter.stderr.println("run with '--help' for more information")
      sys.exit(2)
  }

  @deprecated("use parseOrExit instead", "0.7.2")
  def parse(args: Seq[String]): Unit = parseOrExit(args)
  @deprecated("use parseOrExit instead", "0.7.2")
  def parse(args: Array[String]): Unit = parseOrExit(args.toSeq)

}
