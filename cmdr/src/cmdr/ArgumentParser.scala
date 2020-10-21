package cmdr

import scala.collection.mutable

/** An arg represents a handle to a parameter's value. */
trait Arg[A] {

  @deprecated("use apply() instead", "0.2.1")
  def get: A = apply()

  /** Get the value of this argument. Note that this will throw
    * if the argument has not yet been parsed.
    */
  def apply(): A
}

object ArgumentParser {
  def apply(prog: String = "", description: String = "", version: String = "") =
    new ArgumentParser(prog, description, version)
}

/** A simple command line argument parser.
  *
  * = Usage =
  *
  * 1. Define parameters with [[param]], [[requiredParam]] and [[repeatedParam]].
  *    Each of these methods gives back a handle to a future argument value.
  *
  * 2. Call `parse()` with actual arguments.
  *
  * 3a. If parsing succeeds, the arguments will be available in the handles
  *     defined in step 1.
  *
  * 3b. If parsing fails, error descriptions are printed and the program exits
  *     with 2. (This behaviour may be changed by subclassing and redefining
  *     the `check()` method).
  *
  * = Example =
  *
  * {{{
  * val parser = cmdr.ArgumentParser("appname", "0.1.0")
  * val p1 = parser.param[String]("--this-is-a-named-param", "default value")
  * val p2 = parser.param[Int]("positional-param", 2)
  * parser.parse(Seq("--this-is-a-named-param=other", 5))
  * println(p1())
  * println(p2())
  * }}}
  */
class ArgumentParser(
    prog: String,
    description: String,
    version: String
) {

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
  protected def showAndExit(msg: String): Unit = {
    System.err.println(msg)
    sys.exit(0)
  }
  protected def check(): Unit = {
    if (errors > 0) {
      System.err.println(s"try '$prog --help' for more information")
      sys.exit(2)
    }
  }

  private val named = mutable.ListBuffer.empty[ParamDef]
  private val positional = mutable.ArrayBuffer.empty[ParamDef]

  private val aliases = mutable.Map.empty[String, ParamDef] // used for lookups

  private class Completable[A](name: String) extends Arg[A] {
    var isComplete = false
    var _value: A = _
    def apply(): A =
      if (!isComplete) {
        throw new NoSuchElementException(
          s"This argument is not yet available. Make sure to call ArgumentParser#parse(args) before accessing this value."
        )
      } else {
        _value
      }
  }
  private case class ParamDef(
      names: Seq[String], // first element is the primary name used in messages
      hasDefault: Boolean,
      env: Option[String],
      help: String,
      isFlag: Boolean, // flags never take an argument (unless embedded), and are assigned the string "true" if present
      allowRepeat: Boolean,
      useDefault: () => Unit,
      parseAndSet: (String) => Unit,
      // set to true to treat all subsequent parameters as positionals, regardless of their name;
      // this can be useful for constructing nested commands
      absorbRemaining: Boolean = false
  ) {
    require(names.size > 0, "a parameter must have at least one name")
    def name = names.head
    def isNamed = name.startsWith("-")

    def pretty = {
      val base = if (isNamed && isFlag) {
        name
      } else if (isNamed) {
        s"$name=<value>"
      } else {
        s"<$name>"
      }
      if (allowRepeat) {
        s"[$base...]"
      } else if (hasDefault) {
        s"[$base]"
      } else base
    }

    override def toString = name
  }
  private def addParamDef(p: ParamDef) = {
    if (p.isNamed) {
      named += p
      p.names.foreach { s => aliases += s -> p }
    } else {
      positional += p
    }
  }

  private def addParam[A](
      name: String,
      default: Option[A],
      env: Option[String],
      aliases: Seq[String],
      help: String,
      flag: Boolean,
      absorbRemaining: Boolean
  )(implicit reader: Reader[A]): Arg[A] = {
    val handle = new Completable[A](name)
    val p = ParamDef(
      Seq(name) ++ aliases,
      default.isDefined,
      env,
      help,
      flag,
      false,
      () => {
        handle._value = default.get
        handle.isComplete = true
      },
      (value: String) =>
        reader.read(value) match {
          case Left(message) => reportParseError(name, message)
          case Right(value) =>
            handle._value = value
            handle.isComplete = true

        },
      absorbRemaining
    )
    addParamDef(p)
    handle
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
      default: A,
      env: String = null,
      aliases: Seq[String] = Seq.empty,
      help: String = "",
      flag: Boolean = false,
      absorbRemaining: Boolean = false
  )(
      implicit reader: Reader[A]
  ): Arg[A] =
    addParam(
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
    addParam(name, None, Option(env), aliases, help, flag, absorbRemaining)

  /** Define a parameter that may be repeated.
    *
    * Note that all named parameters may be repeated, regardless if they are
    * defined as repeated or not. The difference is that for non-repeat-defined
    * parameters the last value is used, whereas repeat-defined parameters
    * accumulate values. (This is why [[repeatedParam]] takes an `A` but gives
    * back a `Seq[A]`, while other params take `A` and give back `A`).
    *
    * E.g. consider the command line `--foo=1 --foo=2 --foo=3`
    *
    * In case foo is a regular named parameter, then, after parsing, the value
    * will be `3`. In case it is defined as a repeating parameter, its value will
    * be `Seq(1,2,3)`.
    *
    * Repeated positional parameters consume all remaining positional command line
    * arguments.
    */
  def repeatedParam[A](
      name: String,
      aliases: Seq[String] = Seq.empty,
      help: String = "",
      flag: Boolean = false
  )(implicit reader: Reader[A]): Arg[Seq[A]] = {
    var values = mutable.ArrayBuffer.empty[A]
    var isDone = false
    val p = ParamDef(
      Seq(name) ++ aliases,
      hasDefault = true,
      env = None,
      help = help,
      isFlag = flag,
      allowRepeat = true,
      useDefault = () => isDone = true, // do nothing, default is an empty collection
      parseAndSet = (value: String) => {
        reader.read(value) match {
          case Left(message) => reportParseError(name, message)
          case Right(value) =>
            values += value
            isDone = true
        }
      }
    )
    addParamDef(p)
    new Arg[Seq[A]] {
      def apply(): Seq[A] = values.toList
    }
  }

  def help: String = {
    val b = new StringBuilder
    b ++= s"Usage: $prog "
    if (!named.isEmpty) {
      b ++= "[OPTIONS] "
    }
    for (param <- positional) {
      b ++= s"<${param.name}>"
      if (param.allowRepeat) b ++= "..."
      b ++= " "
    }
    b ++= "\n\n"
    b ++= description
    b ++= "\n"

    // Note that not necessarily all named parameters must be optional. However
    // since that is usually the case, this is what the default help message
    // assumes.
    b ++= "\nOptions:\n"
    val optLines = mutable.ListBuffer.empty[String]
    for (param <- named) {
      val names = if (param.isFlag) {
        param.names.mkString(", ")
      } else {
        param.names.map(_ + "=").mkString(", ")
      }
      optLines += f"  $names%-20s ${param.help}%-50s\n"
    }
    optLines += "  --version            Show the version and exit\n"
    optLines += "  --help               Show this message and exit\n"

    for (line <- optLines.sorted) b ++= line

    val envVars = named.filter(_.env.isDefined)
    if (!envVars.isEmpty) {
      b ++= "\nEnvironment:\n"
      for (param <- envVars) {
        b ++= f"  ${param.env.get}%-20s ${param.name}%-50s\n"
      }
    }

    b.result()
  }

  private val Named = "(--?[^=]+)(?:=(.*))?".r

  def parse(args: Array[String]): Unit = parse(args.toSeq)

  /** Parse the given arguments with respect to the parameters defined by
    * [[param]], [[requiredParam]] and [[repeatedParam]].
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
    val namedArgs = mutable.Map.empty[String, mutable.ListBuffer[String]]

    val positionalArgs = mutable.ArrayBuffer.empty[String]
    var pos = 0 // having this as a separate var from positionalArgs.length allows processing repeated params

    // first, iterate over all arguments to detect extraneous ones
    val argIter = args.iterator
    var arg: String = null
    def readArg() = if (argIter.hasNext) arg = argIter.next() else arg = null
    readArg()

    var onlyPositionals = false
    def addPositional(arg: String) =
      if (pos < positional.length) {
        positionalArgs += arg
        if (positional(pos).absorbRemaining) onlyPositionals = true
        if (!positional(pos).allowRepeat) pos += 1
      } else {
        reportUnknown(arg)
      }
    while (arg != null) {
      if (onlyPositionals) {
        addPositional(arg)
        readArg()
      } else {
        arg match {
          case "--" =>
            onlyPositionals = true
            readArg()
          case "--help" =>
            showAndExit(help)
            readArg()
          case "--version" =>
            showAndExit(version)
            readArg()
          case Named(name0, embedded) if aliases.contains(name0) =>
            readArg()
            val param = aliases(name0)
            val name = param.name // ensure that name is long, even if it came from a short
            namedArgs.getOrElseUpdate(name, mutable.ListBuffer.empty[String])
            if (embedded != null) { // embedded argument, i.e. one that contains '='
              namedArgs(name) += embedded
            } else if (param.isFlag) { // flags never take an arg and are set to "true"
              namedArgs(name) += "true"
            } else if (arg == null || arg.matches(Named.regex)) { // non-flags must have an arg
              reportParseError(name, "argument expected")
            } else {
              namedArgs(name) += arg
              readArg()
            }
            if (param.absorbRemaining) onlyPositionals = true
          case Named(name, _) =>
            reportUnknown(name)
            readArg()
          case positional =>
            addPositional(positional)
            readArg()
        }
      }
    }

    // then, iterate over all parameters to detect missing arguments
    for (param <- named) {
      val envFallback: Option[String] = param.env.flatMap(sys.env.get(_))

      namedArgs.getOrElse(param.name, mutable.ListBuffer.empty).result() match {
        case Nil if envFallback.isDefined => param.parseAndSet(envFallback.get)
        case Nil if param.hasDefault      => param.useDefault()
        case Nil                          => reportMissing(param.name)
        case list                         => for (l <- list) param.parseAndSet(l)
      }
    }

    for (i <- 0 until pos) {
      positional(i).parseAndSet(positionalArgs(i))
    }
    for (i <- pos until positionalArgs.length) {
      positional(pos).parseAndSet(positionalArgs(i))
    }
    for (i <- pos until positional.length) {
      val p = positional(i)
      if (p.hasDefault) {
        p.useDefault()
      } else {
        reportMissing(positional(i).name)
      }
    }

    check()
    // at this point, all values are populated
  }

}
