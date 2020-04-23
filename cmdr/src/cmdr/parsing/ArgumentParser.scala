package cmdr.parsing

import scala.collection.mutable

/** An arg represents a handle to a parameter's value. */
trait Arg[A] {

  /** Get the value of this argument. Note that this will throw
    * if the argument has not yet been parsed.
    */
  def get: A
}

object ArgumentParser {
  def apply(prog: String = "", version: String = "", help: String = "") =
    new ArgumentParser(prog, version, help)
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
  * println(p1.get)
  * println(p2.get)
  * }}}
  */
class ArgumentParser(
    prog: String,
    version: String,
    description: String
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

  private val named = mutable.Map.empty[String, ParamDef]
  private val positional = mutable.ArrayBuffer.empty[ParamDef]

  private class Completable[A](name: String) extends Arg[A] {
    var isComplete = false
    var _value: A = _
    def get: A =
      if (!isComplete) {
        throw new NoSuchElementException(
          s"This argument is not yet available. Make sure to call ArgumentParser#parse(args) before accessing this value."
        )
      } else {
        _value
      }
  }
  private case class ParamDef(
      name: String,
      hasDefault: Boolean,
      env: Option[String],
      help: String,
      allowRepeat: Boolean,
      useDefault: () => Unit,
      parseAndSet: (String) => Unit
  ) {
    override def toString = name
  }
  private def addParamDef(p: ParamDef) = {
    if (p.name.startsWith("--")) {
      named += p.name -> p
    } else {
      positional += p
    }
  }

  private def addParam[A](
      name: String,
      default: Option[A],
      env: Option[String],
      help: String
  )(implicit reader: Reader[A]): Arg[A] = {
    val handle = new Completable[A](name)
    val p = ParamDef(
      name,
      default.isDefined,
      env,
      help,
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

        }
    )
    addParamDef(p)
    handle
  }

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
      help: String = ""
  )(implicit reader: Reader[A]): Arg[Seq[A]] = {
    var values = mutable.ArrayBuffer.empty[A]
    var isDone = false
    val p = ParamDef(
      name,
      hasDefault = true,
      env = None,
      help = help,
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
      def get: Seq[A] = values.toList
    }
  }

  /** Define an optional parameter, using the given default value if it is not
    * supplied on the command line or by an environment variable.
    *
    * ErgoTip: always give named parameters a default value.
    *
    * ''Internal design note: [[param]] and [[requiredParam]] differ only in the presence and absence
    * of the 'default' parameter. Ideally, they would be merged into one single
    * method, giving the 'default' parameter a default null value (as is done for
    * the other optional parameters, such as 'env' and 'help'). Unfortunately,
    * since 'default' is of type A where A may be a primitive type, it cannot
    * be assigned null. The usual solution would be to wrap it in an Option type,
    * but that leads to an ugly API. Hence the method is split into two.
    * See addParam() for the common denominator.''
    *
    * @tparam A The type to which an argument shall be converted.
    *
    * @param name The name of the parameter. A name starting with `--` indicates
    * a named parameter, whereas any other name indicates a positional parameter.
    *
    * @param default The default value to use in case no matching argument is provided.
    *
    * @param env The name of an environment variable from which to read the argument
    * in case it is not supplied on the command line. Set to 'null' to ignore.
    *
    * @param help A help message to display when the user types `--help`
    *
    * @return A handle to the parameter's future value, available once `parse(args)` has been called.
    */
  def param[A](name: String, default: A, env: String = null, help: String = "")(
      implicit reader: Reader[A]
  ): Arg[A] = addParam(name, Some(default), Option(env), help)

  /** Define a required parameter.
    *
    * This method is similar to [[param]], except that it does not accept a
    * default value. Instead, missing arguments for this parameter will cause
    * the parser to fail.
    *
    * ErgoTip: avoid requiring named parameters. Only require positional parameters.
    *
    * @see param
    */
  def requiredParam[A](name: String, env: String = null, help: String = "")(
      implicit reader: Reader[A]
  ): Arg[A] = addParam(name, None, Option(env), help)

  private def help: String = {
    val b = new StringBuilder
    b ++= s"usage: $prog ${named.keySet.mkString(" ")} ${positional.map(_.name).mkString(" ")}\n"
    b.result()
  }

  private val Named = "(--[^=]+)=?(.*)?".r

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

    def addPositional(arg: String) =
      if (pos < positional.length) {
        positionalArgs += arg
        if (!positional(pos).allowRepeat) pos += 1
      } else {
        reportUnknown(arg)
      }

    // first, iterate over all arguments to detect extraneous ones
    var onlyPositionals = false
    for (arg <- args) {
      if (onlyPositionals) {
        addPositional(arg)
      } else {
        arg match {
          case "--"            => onlyPositionals = true
          case "-h" | "--help" => showAndExit(help)
          case "--version"     => showAndExit(version)
          case Named(name, value) if named.contains(name) =>
            val prev =
              namedArgs.getOrElseUpdate(name, mutable.ListBuffer.empty[String])
            namedArgs(name) += value
          case Named(name, _) =>
            reportUnknown(name)
          case positional => addPositional(positional)
        }
      }
    }

    // then, iterate over all parameters to detect missing arguments
    for ((name, param) <- named) {
      val envFallback: Option[String] = param.env.flatMap(sys.env.get(_))

      namedArgs.getOrElse(name, mutable.ListBuffer.empty).result match {
        case Nil if envFallback.isDefined => param.parseAndSet(envFallback.get)
        case Nil if param.hasDefault      => param.useDefault()
        case Nil                          => reportMissing(name)
        case list if param.allowRepeat    => for (l <- list) param.parseAndSet(l)
        case list =>
          param.parseAndSet(
            list.last
          ) // last occurence of named params override
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
