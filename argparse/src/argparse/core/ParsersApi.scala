package argparse
package core

import scala.collection.mutable
import Parser.ParamDef


trait ParsersApi { api: TypesApi =>

  /** Generate a help message from parameters.
    *
    * This message will be used by ArgumentParsers. Overriding this allows you
    * to customize the help message of all ArgumentParsers.
    */
  def help(
    description: String,
    params: Seq[ParamInfo],
    commands: Seq[CommandInfo]
  ): String = {
    val (named0, positional) = params.partition(_.isNamed)
    val named = named0.sortBy(_.names.head)

    val width = math.max(argparse.term.cols - 20, 20)

    val b = new StringBuilder
    b ++= s"usage: "
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

    if (!commands.isEmpty) {
      b ++= "commands:\n"
      for (cmd <- commands) {
        b ++= f"  ${cmd.name}%-20s"
        TextUtils.wrap(cmd.description, b, width, "\n                     ")
        b ++= "\n"
      }
    }

    val describedPos = positional.filter(!_.description.isEmpty)
    if (!describedPos.isEmpty && commands.isEmpty) {
      b ++= "positional arguments:\n"
      for (param <- positional) {
        b ++= s"  ${param.names.head}\n        "
        TextUtils.wrap(param.description, b, width, "\n        ")
        b ++= "\n"
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
      b ++= s"  $names"
      param.argName.foreach(n => b ++= s"<$n>")
      b ++= "\n        "
      TextUtils.wrap(param.description, b, width, "\n        ")
      b ++= "\n"
    }

    val envVars = named.filter(_.env.isDefined)
    if (!envVars.isEmpty) {
      b ++= "environment variables:\n"
      for (param <- envVars) {
        b ++= f"  ${param.env.get}%-30s sets ${param.names.head}%s%n"
      }
    }

    b.result()
  }

  /** The name of the flag to use for generating standalone bash-completion.
    *
    * Set this to the empty string to disable bash-completion entirely.
    */
  def bashCompletionFlag = "--bash-completion"

  object ArgumentParser {
    def apply(
        description: String = "",
        enableHelpFlag: Boolean = true,
        enableBashCompletionFlag: Boolean = true,
        stdout: java.io.PrintStream = System.out,
        stderr: java.io.PrintStream = System.err,
        env: Map[String, String] = sys.env
    ) = new ArgumentParser(description, enableHelpFlag, enableBashCompletionFlag, stdout, stderr, env)

    sealed trait Result
    /** Parsing succeeded. Arguments are available. */
    case object Success extends Result

    /** There was an error during parsing. Arguments are not available. */
    case object Error extends Result

    /** Parsing signalled an early exit. This means that there wasn't an error,
      * but that not all arguments were parsed. This occurs if one of the
      * arguments requested an early exit after some side-effect (for example,
      * `--help` will print a help message and then signal an early exit).
      * Arguments are not available. */
    case object EarlyExit extends Result

  }

  /** A simple command line argument parser.
    *
    * Usage:
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
    * Example
    *
    * ```
    * val parser = argparse.ArgumentParser("0.1.0")
    *
    * val p1 = parser.param[String]("--this-is-a-named-param", default = "default value")
    * val p2 = parser.param[Int]("positional-param", default = 2)
    *
    * parser.parse(Seq("--this-is-a-named-param=other", 5)) println(p1())
    *
    * println(p2())
    * ```
    *
    * @param description a short description of this command. Used in help
    * messages.
    * @param enableHelpFlag include a `--help` flag which will print a generated help message
    */
  class ArgumentParser(
      val description: String,
      val enableHelpFlag: Boolean,
      val enableBashCompletionFlag: Boolean,
      val stdout: java.io.PrintStream,
      val stderr: java.io.PrintStream,
      val env: Map[String, String]
  ) { self =>
    import ArgumentParser._

    private var errors = 0

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

    protected def reportParseError(name: String, message: String): Unit = {
      stderr.println(s"error processing argument $name: $message")
      errors += 1
    }

    protected def reportUnknownCommand(actual: String, available: Seq[String]) = {
      stderr.println("unknown command: " + actual)
      stderr.println("expected one of: " + available.mkString(", "))
      errors += 1
    }

    protected def hasErrors = errors > 0 // TODO: make public?

    private val paramDefs = mutable.ListBuffer.empty[ParamDef]
    private val paramInfos = mutable.ListBuffer.empty[ParamInfo]
    private val commandInfos = mutable.ListBuffer.empty[CommandInfo]

    // /** Low-level escape hatch for manually adding parameter definitions.
    //   *
    //   * See also [[param]], [[requiredParam]] and [[repeatedParam]] for the
    //   * high-level API.
    //   */
    // def addParamDef(pdef: ParamDef): Unit = paramDefs += pdef

    // /** Low-level escape hatch for manually adding parameter information.
    //   *
    //   * See also [[param]], [[requiredParam]] and [[repeatedParam]] for the
    //   * high-level API.
    //   */
    // def addParamInfo(pinfo: ParamInfo): Unit = paramInfos += pinfo

    if (enableHelpFlag) {
      paramDefs += ParamDef(
        Seq("--help"),
        (_, _) => {
          stdout.println(help())
          Parser.Stop
        },
        missing = () => (),
        isFlag = true,
        repeatPositional = false,
        endOfNamed = false
      )
      // paramInfos += ParamInfo(
      //   isNamed = true,
      //   names = Seq("--help"),
      //   argName = None,
      //   repeats = false,
      //   env = None,
      //   description = "show this message and exit",
      //   interactiveCompleter = _ => Seq.empty,
      //   standaloneCompleter = BashCompleter.Empty
      // )
    }

    /** A default help message, generated from parameter help strings. */
    def help(): String = api.help(description, paramInfos.toSeq, commandInfos.toSeq)

    if (enableBashCompletionFlag && bashCompletionFlag != "") {
      paramDefs += ParamDef(
        Seq(bashCompletionFlag),
        (p, name) => {
          name match {
            case None => reportParseError(p, "argument required: name of program to complete")
            case Some(name) => printBashCompletion(name)
          }
          Parser.Stop
        },
        missing = () => (),
        isFlag = false,
        repeatPositional = false,
        endOfNamed = false
      )
      // paramInfos += ParamInfo(
      //   isNamed = true,
      //   names = Seq(bashCompletionFlag),
      //   argName = None,
      //   repeats = false,
      //   env = None,
      //   description = "generate bash completion for this command",
      //   interactiveCompleter = _ => Seq.empty,
      //   standaloneCompleter = BashCompleter.Empty
      // )
    }

    def printBashCompletion(programName: String): Unit = {
      try {
        StandaloneBashCompletion.completeAndThrow(
          stdout,
          paramInfos.toList,
          commandInfos.toList,
          Seq("---nested-completion", programName)
        )
      } catch {
        case _: StandaloneBashCompletion.CompletionReturned =>
      }
    }

    def singleParam[A](
        name: String,
        default: Option[() => A],
        env: Option[String],
        aliases: Seq[String],
        help: String,
        flag: Boolean,
        endOfNamed: Boolean,
        interactiveCompleter: Option[String => Seq[String]],
        standaloneCompleter: Option[BashCompleter],
        argName: Option[String]
    )(implicit reader: Reader[A]): argparse.Argument[A] = {
      val arg = new argparse.Argument[A](name)

      def read(name: String, strValue: String): Unit = {
        reader.read(strValue) match {
          case Reader.Error(message) => reportParseError(name, message)
          case Reader.Success(value) => arg.set(value)
        }
      }

      def parseAndSet(name: String, valueOpt: Option[String]) = {
        valueOpt match {
          case Some(v) => read(name, v)
          case None    => reportParseError(name, "argument expected")
        }
        Parser.Continue
      }

      val pdef = ParamDef(
        names = Seq(name) ++ aliases,
        parseAndSet = parseAndSet,
        missing = () => {
          val fromEnv = env.flatMap(self.env.get(_))

          fromEnv match {
            case Some(str) => parseAndSet(s"from env ${env.get}", Some(str))
            case None if default.isDefined =>
              arg.set(default.get())
            case None => reportMissing(name)
          }
        },
        isFlag = flag,
        repeatPositional = false,
        endOfNamed = endOfNamed
      )
      paramDefs += pdef

      paramInfos += ParamInfo(
        isNamed = pdef.isNamed,
        names = pdef.names,
        argName = if (flag) None else argName.orElse(Some(reader.typeName)),
        repeats = false,
        env = env,
        description = help,
        interactiveCompleter = interactiveCompleter.getOrElse(reader.interactiveCompleter),
        standaloneCompleter = standaloneCompleter.getOrElse(reader.standaloneCompleter)
      )

      arg
    }

    /** Define an optional parameter, using the given default value if it is not
      * supplied on the command line or by an environment variable.
      *
      * *ErgoTip: always give named parameters a default value.*
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
      * @param endOfNamed Indicates that any arguments encountered after this parameter
      * must be treated as positionals, even if they start with `-`. In other words, a
      * parameter marked with this has the same effect as the `--` separator. It can be
      * useful for implementing sub-commands. (Note however that this ArgumentParser has a
      * dedicated `command` method for such use cases)
      *
      * @param interactiveCompleter A bash snippet that is inserted in bash-completions, responsible for setting
      * completion options for this param. If omitted, the parameter type's (A) default interactiveCompleter
      * will be used. If present, this must be valid bash and should set COMPREPLY. The bash variable
      * "$cur" may be used in the snippet, and will contain the current word being completed for this
      * parameter.
      *
      * @param argName The name to use in help messages for this parameter's argument.
      * This only has an effect on named parameters which take an argument. By default,
      * the name of the type of the argument will be used.
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
        endOfNamed: Boolean = false,
        interactiveCompleter: String => Seq[String] = null,
        standaloneCompleter: BashCompleter = null,
        argName: String = null
    )(
        implicit reader: Reader[A]
    ): argparse.Argument[A] =
      singleParam(
        name,
        Some(() => default),
        Option(env),
        aliases,
        help,
        flag,
        endOfNamed,
        Option(interactiveCompleter),
        Option(standaloneCompleter),
        Option(argName)
      )

    /** Define a required parameter.
      *
      * This method is similar to [[param]], except that it does not accept a
      * default value. Instead, missing arguments for this parameter will cause
      * the parser to fail.
      *
      * *ErgoTip: avoid named parameters that are required. Only require
      * positional parameters.*
      *
      * @see param
      */
    def requiredParam[A](
        name: String,
        env: String = null,
        aliases: Seq[String] = Seq.empty,
        help: String = "",
        flag: Boolean = false,
        endOfNamed: Boolean = false,
        interactiveCompleter: String => Seq[String] = null,
        standaloneCompleter: BashCompleter = null,
        argName: String = null
    )(
        implicit reader: Reader[A]
    ): argparse.Argument[A] =
      singleParam(
        name,
        None,
        Option(env),
        aliases,
        help,
        flag,
        endOfNamed,
        Option(interactiveCompleter),
        Option(standaloneCompleter),
        Option(argName)
      )(reader)

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
        interactiveCompleter: String => Seq[String] = null,
        standaloneCompleter: BashCompleter = null,
        argName: String = null
    )(implicit reader: Reader[A]): argparse.Argument[Seq[A]] = {
      val arg = new argparse.Argument[Seq[A]](name)
      var values = mutable.ListBuffer.empty[A]
      arg.set(values.toList)

      def read(name: String, strValue: String): Unit = {
        reader.read(strValue) match {
          case Reader.Error(message) => reportParseError(name, message)
          case Reader.Success(value) =>
            values += value
            arg.set(values.toList)
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
          Parser.Continue
        },
        missing = () => (),
        flag,
        repeatPositional = true,
        endOfNamed = false
      )
      paramDefs += pdef

      paramInfos += ParamInfo(
        isNamed = pdef.isNamed,
        names = pdef.names,
        argName = if (flag) None else if (argName == null) Some(reader.typeName) else None,
        repeats = true,
        env = None,
        description = help,
        interactiveCompleter = Option(interactiveCompleter).getOrElse(reader.interactiveCompleter),
        standaloneCompleter = Option(standaloneCompleter).getOrElse(reader.standaloneCompleter)
      )

      arg
    }

    /** Utility to define a sub command.
      *
      * Many modern command line apps actually consist of multiple nested
      * commands, each corresponding to the verb of an action, such as 'run' or
      * 'clone'. Typically, each sub command also has its own dedicated parameters
      * list.
      *
      * In argparse, subcommands can easily be modelled by a positional parameter that
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
    def parseResult(args: Iterable[String]): Result = {

      var _command: () => String = null
      var _commandArgs: () => Seq[String] = null

      if (!commandInfos.isEmpty) {
        val commands = commandInfos.map(_.name)
        _command = requiredParam[String](
          "command",
          endOfNamed = true,
          interactiveCompleter = prefix => commands.filter(_.startsWith(prefix)).toList,
          standaloneCompleter = BashCompleter.Fixed(commands.toSet)
        )
        _commandArgs = repeatedParam[String](
          "args"
        )
      }

      if (InteractiveBashCompletion.completeOrFalse(
            paramInfos.toList,
            commandInfos.toList,
            env,
            args,
            stdout
          )) {
        return EarlyExit
      }
      StandaloneBashCompletion.completeAndThrow(
        stdout,
        paramInfos.toList,
        commandInfos.toList,
        args
      )

      if (!Parser.parse(
        paramDefs.result(),
        args,
        reportUnknown
      )) return EarlyExit


      if (hasErrors) return Error

      if (!commandInfos.isEmpty) {
        commandInfos.find(_.name == _command()) match {
          case Some(cmd) =>
            cmd.action(_commandArgs())
          case None =>
            reportUnknownCommand(
              _command(),
              commandInfos.map(_.name).result()
            )
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
      case Success   => ()
      case EarlyExit => sys.exit(0)
      case Error =>
        stderr.println("run with '--help' for more information")
        sys.exit(2)
    }
  }

}
