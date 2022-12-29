package argparse
package core

import scala.collection.mutable
import Parser.ParamDef
import scala.annotation.meta.param

trait ParsersApi { api: TypesApi =>

  /** Generate a help message from parameters.
    *
    * This message will be used by `ArgumentParser`s. Overriding this allows you
    * to customize the help message of all `ArgumentParser`s.
    */
  def defaultHelpMessage(parser: ParsersApi#ArgumentParser): String = {
    val (named0, positional) = parser.paramInfos.partition(_.isNamed)
    val named = named0.sortBy(_.names.head)

    val b = new StringBuilder
    b ++= s"Usage:"
    if (!named.isEmpty) {
      b ++= " [OPTIONS]"
    }
    for (param <- positional) {
      b ++= " "
      b ++= param.names.head.toUpperCase
      if (param.repeats) b ++= "..."
    }
    b ++= "\n"

    if (!parser.description.isEmpty()) {
      b ++= "\n"
      b ++= parser.description
      b ++= "\n\n"
    }

    // Note that not necessarily all named parameters must be optional. However
    // since that is usually the case, this is what the default help message
    // assumes.
    if (!named.isEmpty) {
      b ++= "Options:\n"

      // -short, --long tpe wrapped
      val lhs = for (param <- named) yield {
        val long = param.names.head
        val short = if (long.length == 2) "" else param.names.find(_.length == 2).getOrElse("")
        val argname = param.argName.getOrElse("")

        if (short != "") {
          s"  $short, $long $argname  "
        } else {
          s"      $long $argname  "
        }
      }

      val col1Width = lhs.map(_.length).max
      val col2Width = argparse.term.cols - col1Width

      if (col2Width > 30) {
        for ((l, param) <- lhs.zip(named)) {
          b ++= l
          b ++= " " * (col1Width - l.length)
          TextUtils.wrap(param.description, b, col2Width, "\n" + " " * col1Width)
          b += '\n'
        }
      } else {
        for ((l, param) <- lhs.zip(named)) {
          b ++= l
          b += '\n'
          b ++= param.description
          b += '\n'
        }
      }
    }

    if (!parser.subparsers.isEmpty) {
      val width = parser.subparsers.keySet.map(_.length).max + 3

      b ++= "Commands:\n"
      for ((name, cmd) <- parser.subparsers) {
        b ++= "  "
        b ++= name
        b ++= " "
        b ++= " " * (width - name.length)
        TextUtils.wrap(cmd.description, b, argparse.term.cols - width, "\n" + " " * width)
        b ++= "\n"
      }
    }

    // val describedPos = positional.filter(!_.description.isEmpty)
    // if (!describedPos.isEmpty) {
    //   b ++= "positional arguments:\n"
    //   for (param <- positional) {
    //     b ++= s"  ${param.names.head}\n        "
    //     TextUtils.wrap(param.description, b, width, "\n        ")
    //     b ++= "\n"
    //   }
    // }

    val envVars = named.filter(_.env.isDefined)
    if (!envVars.isEmpty) {
      val envWidth = envVars.map(_.env.get.length).max + 2
      b ++= "Environment variables:\n"
      for (param <- envVars) {
        b ++= "  "
        b ++= param.env.get
        b ++= " " * (envWidth - param.env.get.length)
        b ++= " "
        b ++= s"sets ${param.names.head}\n"
      }
    }

    b.result()
  }

  /** The name of the flag to use for generating standalone bash-completion.
    *
    * Set this to empty to disable bash-completion entirely.
    *
    * Note that individual argument parsers may override this.
    */
  def defaultBashCompletionFlags = Seq("--bash-completion")

  /** The name of the flag to use for printing help messages.
    *
    * Set this to empty to disable help entirely.
    *
    * Note that individual argument parsers may override this.
    */
  def defaultHelpFlags = Seq("--help")

  /** Called by parseOrExit in case of error.
    *
    * Overriding this can be useful in situations where you do not want to exit,
    * for example in tests. */
  protected def exit(code: Int): Nothing = sys.exit(code)

  object ArgumentParser {
    def apply(
        description: String = "",
        helpFlags: Seq[String] = defaultHelpFlags,
        bashCompletionFlags: Seq[String] = defaultBashCompletionFlags
    ) = new ArgumentParser(description, helpFlags, bashCompletionFlags)
  }

  /** A simple command line argument parser.
    *
    * Usage:
    *
    * 1. Define parameters with [[param]], [[requiredParam]] and
    *    [[repeatedParam]]. Each of these methods gives back a handle to a
    *    future argument value.
    *
    * 2. Call `parseOrExit()` with actual arguments.
    *
    * 3. If parsing succeeds, the arguments will be available in the handles
    *    defined in step 1.
    *
    *    If parsing fails, error descriptions are printed and the program exits
    *    with 2.
    *
    * Example:
    *
    * ```scala
    * val parser = argparse.default.ArgumentParser()
    *
    * val p1 = parser.param[String]("--this-is-a-named-param", default = "default value")
    * val p2 = parser.param[Int]("positional-param", default = 2)
    *
    * parser.parseOrExit(Seq("--this-is-a-named-param=other", 5))
    * println(p1.value)
    * println(p2.value)
    * ```
    *
    * @param description A short description of this command. Used in help
    * messages.
    * @param helpFlags Use these flags to print the help message. Set to empty
    * to disable.
    * @param bashCompletionFlag Use these flags to print a sourceable
    * bash-completion script. Set to empty to disable.
    */
  class ArgumentParser(
      val description: String,
      val helpFlags: Seq[String],
      val bashCompletionFlags: Seq[String]
  ) { self =>

    private var _env: Map[String, String] = Map()
    private var _stdout: java.io.PrintStream = null
    private var _stderr: java.io.PrintStream = null

    def env = _env
    def stdout = _stdout
    def stderr = _stderr

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

    protected def reportUnknownCommand(actual: String) = {
      stderr.println("unknown command: " + actual)
      errors += 1
    }

    protected def reportPostCheck(message: String) = {
      stderr.println("error: " + message)
      errors += 1
    }

    protected def hasErrors = errors > 0

    private val _paramDefs = mutable.ListBuffer.empty[ParamDef]
    private val _paramInfos = mutable.ListBuffer.empty[ParamInfo]
    private val _subparsers = mutable.Map.empty[String, argparse.core.ParsersApi#ArgumentParser]
    private val _subparserAliases = mutable.Map.empty[String, argparse.core.ParsersApi#ArgumentParser]
    private val _postChecks = mutable.ListBuffer.empty[(Iterable[String], Map[String, String]) => Option[String]]

    /** The actual parameters. These objects contains callbacks that are invoked
      * by the parser.
      *
      * This is a low-level escape hatch. You should prefer declaring parameters
      * via the `param()`, `requiredParam()` and `repeatedParam()` methods.
      */
    def paramDefs: List[ParamDef] = _paramDefs.toList

    /** Human-readable information about parameters. These objects do not
      * influence parsing, but contain additional information that is useful to
      * generate help messages and bash completion.
      *
      * This is a low-level escape hatch. You should prefer declaring parameters
      * via the `param()`, `requiredParam()` and `repeatedParam()` methods.
      */
    def paramInfos = _paramInfos.toList

    /** Nested parsers that have been declared in this parser.
      *
      * This is a low-level escape hatch. You should prefer declaring
      * subcommands via the `command()` method.
      */
    def subparsers = _subparsers.toMap

    /** Low-level escape hatch for manually adding parameter definitions.
      *
      * You should prefer declaring parameters via the `param()`,
      * `requiredParam()` and `repeatedParam()` methods.
      */
    def addParamDef(pdef: ParamDef): Unit = _paramDefs += pdef

    /** Low-level escape hatch for manually adding parameter information.
      *
      * You should prefer declaring parameters via the `param()`,
      * `requiredParam()` and `repeatedParam()` methods.
      */
    def addParamInfo(pinfo: ParamInfo): Unit = _paramInfos += pinfo

    /** Low-level escape hatch for manually adding a nested parser.
      *
      * You should use the [[subparser()]] method if you want to construct an
      * argument parser of the same API package. This method allows you to nest
      * ArgumentParsers of different API styles.
      */
    def addSubparser(name: String, parser: argparse.core.ParsersApi#ArgumentParser, aliases: Seq[String] = Seq()): this.type = {
      _subparsers += name -> parser
      for (alias <- aliases) {
        _subparserAliases += alias -> parser
      }
      this
    }

    /** Add a function which is run after parsing command line args, optionally
      * reporting an error. */
    def postCheck(
      check: (Iterable[String], Map[String, String]) => Option[String]
    ): this.type = {
      _postChecks += check
      this
    }

    if (!helpFlags.isEmpty) {
      _paramDefs += ParamDef(
        helpFlags,
        (_, _) => {
          stdout.print(help())
          Parser.Stop
        },
        missing = () => (),
        isFlag = true,
        repeatPositional = false,
        endOfNamed = false
      )
      _paramInfos += ParamInfo(
        isNamed = true,
        names = helpFlags,
        argName = None,
        repeats = false,
        env = None,
        description = "show this message and exit",
        interactiveCompleter = _ => Seq.empty,
        standaloneCompleter = BashCompleter.Empty
      )
    }

    private var _help: String = null

    /** The help message. */
    def help(): String = if (_help == null) api.defaultHelpMessage(this) else _help

    def help(message: String): this.type = {
      _help = message
      this
    }

    if (!bashCompletionFlags.isEmpty) {
      _paramDefs += ParamDef(
        bashCompletionFlags,
        (p, name) => {
          name match {
            case None => reportParseError(p, "argument required: name of program to complete")
            case Some(name) => printBashCompletion(stdout, name)
          }
          Parser.Stop
        },
        missing = () => (),
        isFlag = false,
        repeatPositional = false,
        endOfNamed = false
      )
      _paramInfos += ParamInfo(
        isNamed = true,
        names = bashCompletionFlags,
        argName = Some("string"),
        repeats = false,
        env = None,
        description = "generate bash completion for this command",
        interactiveCompleter = _ => Seq.empty,
        standaloneCompleter = BashCompleter.Empty
      )
    }

    /** Generate and print a standalone bash completion script.
      *
      * @param out the stream to which to print the script to
      * @param commandChain the name of the program
      */
    def printBashCompletion(out: java.io.PrintStream, commandChain: String*): Unit = {
      require(commandChain.length >= 1, "the command chain may not be empty")

      if (!_subparsers.isEmpty) {
        command
        commandArgs
      }

      StandaloneBashCompletion.printCommandCompletion(
        commandChain,
        paramInfos,
        out
      )
      for ((name, p) <- _subparsers) {
        p.printBashCompletion(out, (commandChain ++ Seq(name)): _*)
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
      _paramDefs += pdef

      _paramInfos += ParamInfo(
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
      * **ErgoTip: always give named parameters a default value.**
      *
      * *Internal design note: [[param]] and [[requiredParam]] differ only in
      * the presence of the 'default' parameter. Ideally, they would be merged
      * into one single method, giving the 'default' parameter a default null
      * value (as is done for the other optional parameters, such as 'env' and
      * 'help'). Unfortunately, since 'default' is call-by-name, there is no way
      * to check if it has been set to null without evaluating it. See
      * [[singleParam]] for the common denominator.*
      *
      * @tparam A The type to which an argument shall be converted.
      *
      * @param name The name of the parameter. A name starting with `-`
      * indicates a named parameter, whereas any other name indicates a
      * positional parameter. Prefer double-dash named params. I.e. prefer
      * "--foo" over "-foo".
      *
      * @param default The default value to use in case no matching argument is
      * provided.
      *
      * @param env The name of an environment variable from which to read the
      * argument in case it is not supplied on the command line. Set to 'null'
      * to ignore.
      *
      * @param aliases Other names that may be used for this parameter. This is
      * a good place to define single-character aliases for frequently used
      * named parameters. Note that this has no effect for positional
      * parameters.
      *
      * @param help A help message to display when the user types `--help`.
      *
      * @param flag Set to true if the parameter should be treated as a flag.
      *
      * Flags are named parameters that are treated specially by the parser:
      *
      * - they never take arguments, unless the argument is embedded in the flag
      *   itself
      * - they are always assigned the string value "true" if found on the
      *   command line.
      *
      * Note that flags are intended to make it easy to pass boolean parameters;
      * it is quite rare that they are useful for non-boolean params. The flag
      * field has no effect on positional parameters.
      *
      * @param endOfNamed Indicates that any arguments encountered after this
      * parameter must be treated as positionals, even if they start with `-`.
      * In other words, a parameter marked with this has the same effect as the
      * `--` separator. It can be useful for implementing sub-commands. (Note
      * however that this ArgumentParser has a dedicated `command` method for
      * such use cases)
      *
      * @param interactiveCompleter Compute available shell completions starting
      * with a given string. This is used by interactive bash completion, where
      * the user program is responsible for generating completions.
      *
      * @param standaloneBashComplete A completer for bash. This is used by
      * standalone bash completion, where a bash script generates completion,
      * without the involvement of the the user program.
      *
      * If your program is implemented with Scala on the JVM, the startup time
      * is considerable and hence standalone completion should be preferred for
      * a snappy user experience.
      *
      * @param argName The name to use in help messages for this parameter's
      * argument. This only has an effect on named parameters which take an
      * argument. By default, the name of the type of the argument will be used.
      *
      * @return A handle to the parameter's future value, available once
      * `parse(args)` has been called.
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
      * *Note that all named parameters may always be repeated, regardless if
      * they are defined as repeated or not. The difference is that for
      * non-repeat-defined parameters the last value is used, whereas
      * repeat-defined parameters accumulate values. This is why
      * [[repeatedParam]] takes an `A` but gives back a `Seq[A]`, while other
      * params take `A` and give back `A`.*
      *
      * E.g. consider the command line `--foo=1 --foo=2 --foo=3`. In case foo is
      * a regular named parameter, then, after parsing, the value will be `3`.
      * In case it is defined as a repeating parameter, its value will be
      * `Seq(1,2,3)`.
      *
      * Repeated positional parameters consume all remaining positional command
      * line arguments. They should thus only ever be defined as the last
      * positional parameter.
      */
    def repeatedParam[A](
        name: String,
        aliases: Seq[String] = Seq.empty,
        help: String = "",
        flag: Boolean = false,
        endOfNamed: Boolean = false,
        interactiveCompleter: String => Seq[String] = null,
        standaloneCompleter: BashCompleter = null,
        argName: String = null,
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
        endOfNamed = endOfNamed
      )
      _paramDefs += pdef

      _paramInfos += ParamInfo(
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

    /** Utility to define a nested argument parser.
      *
      * Many applications actually consist of multiple nested commands, each
      * corresponding to the verb of an action (such as 'docker run' or 'git
      * clone'). Typically, each nested command also has its own dedicated
      * parameter list.
      *
      * @param name The name of the subcommand.
      * @param description Information about what the nested command does.
      * @param aliases Other names of the subcommand.
      *
      * @return a new [[ArgumentParser]] which will receive any remaining
      * arguments. Note that you should defined an [[action()]] on the new
      * [[ArgumentParser]].
      */
    def subparser(name: String, description: String = "",  aliases: Seq[String] = Seq()): ArgumentParser = {
      val sp = ArgumentParser(
        description = description,
        helpFlags = helpFlags,
        bashCompletionFlags = Seq() // bash completion is handled by the top-level
      )
      addSubparser(name, sp, aliases)
      sp
    }

    private var _subparserUnknown: (String, Iterable[String]) => Unit =
      (command, _) => reportUnknownCommand(command)

    /** Action to run on an unknown subcommand (if subparsers have been
      * defined).
      *
      * You can use it to support dynamic subcommands. E.g.
      *
      * - match on the exact command:
      *
      *   ```
      *   parser.subparserUnknown {
      *     case ("foo", args) => foo(args)
      *   }
      *   ```
      *
      * - run an external command:
      *
      *   ```
      *   parser.subparserUnknown {
      *    case (name, args) => os.proc("app-$name", args)
      *   }
      *   ```
      */
    def subparserUnknown(action: (String, Iterable[String]) => Unit): this.type = {
      _subparserUnknown = action
      this
    }

    private var _action: () => Unit = () => ()

    /** Set an action to be run after successful parsing. */
    def action(fn: => Unit): this.type = {
      _action = () => fn
      this
    }

    /* Note: these are initialized at the latest possible stage, in
     * [[parseResult]] or [[printBashCompletion]]. */
    private lazy val command = requiredParam[String](
      "command",
      endOfNamed = true,
      interactiveCompleter = prefix => _subparsers.keySet.filter(_.startsWith(prefix)).toList.sorted,
      standaloneCompleter = BashCompleter.Fixed(_subparsers.keySet.toSet)
    )
    private lazy val commandArgs = repeatedParam[String]("args")

    /** Parse the given arguments with respect to the parameters defined by
      * [[param]], [[requiredParam]], [[repeatedParam]].
      */
    def parseResult(
      args: Iterable[String],
      env: Map[String, String] = sys.env,
      stdout: java.io.PrintStream = System.out,
      stderr: java.io.PrintStream = System.err
    ): ParseResult = {
      this._env = env
      this._stdout = stdout
      this._stderr = stderr

      if (!_subparsers.isEmpty) {
        command
        commandArgs
      }

      if (InteractiveBashCompletion.completeOrFalse(this, env, stdout)) {
        return ParseResult.EarlyExit
      }

      if (!Parser.parse(_paramDefs.result(), args, reportUnknown)) {
        return ParseResult.EarlyExit
      }

      if (hasErrors) return ParseResult.Error

      for (check <- _postChecks) {
        check(args, env) match {
          case None => // ok, no error
          case Some(error) => reportPostCheck(error)
        }
      }

      if (hasErrors) return ParseResult.Error

      if (!_subparsers.isEmpty) {
        _subparsers.get(command.value).orElse(_subparserAliases.get(command.value)) match {
          case Some(cmd) =>
            return cmd.parseResult(commandArgs.value, env)
          case None =>
            _subparserUnknown(command.value, commandArgs.value)
            return ParseResult.Error
        }
      }

      _action()

      ParseResult.Success
    }

    /** Parse the given arguments with respect to the parameters defined by
      * [[param]], [[requiredParam]], [[repeatedParam]] and [[command]].
      *
      * In case no errors are encountered, the arguments will be populated in
      * the functions returned by the parameter definitions.
      *
      * In case errors are encountered, the default behaviour is to exit the
      * program.
      *
      * The types of errors are:
      *
      * 1. An unknown argument is encountered. This can either be an unspecified
      *    named argument or an extranous positional argument.
      *
      * 2. A required argument is missing.
      *
      * 3. An argument cannot be parsed from its string value to its desired
      *    type.
      *
      * @see parseResult for a version of this function which does not exit
      */
    def parseOrExit(
      args: Iterable[String],
      env: Map[String, String] = sys.env,
      stdout: java.io.PrintStream = System.out,
      stderr: java.io.PrintStream = System.err
    ): Unit = parseResult(args, env, stdout, stderr) match {
      case ParseResult.Success   => ()
      case ParseResult.EarlyExit => exit(0)
      case ParseResult.Error =>
        stderr.println("run with '--help' for more information")
        exit(2)
    }
  }

}
