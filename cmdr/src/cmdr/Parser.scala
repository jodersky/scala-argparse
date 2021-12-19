package cmdr

import scala.collection.mutable

/** Low-level parsing functionality. See ArgParser for a user-friendly API. */
object Parser {

  /** A parameter definition is the low-level building block to define the
    * grammar of a command-line, and its functionality.
    *
    * ParamDefs associate parameter names to actions that are invoked by
    * Parser.parse() in certain situations.
    *
    * @param names All names that may be used by this parameter. If a name
    * starts with `-`, it is considered a "named" parameter, otherwise it is
    * considered a "positional" parameter.
    *
    * Arguments associated to named parameters may appear in any order on the
    * command line, as long as they are prefixed by the parameter's name.
    * Positional parameters are given arguments in the order they appear in.
    *
    * @param parseAndSet A function that is invoked anytime this parameter is
    * encountered on the command line. In case of a named param, the first
    * element is the actual name used, and the second element is the argument or
    * None if no argument followed. In case of a position param, the parameter's
    * first name is given and the argument value is always defined.
    *
    * @param missing A function that is invoked if this parameter has not been
    * encountered at all.
    *
    * @param isFlag Indicates if this named parameter is a flag, i.e. one that
    * never accepts an argument. In case its name is encountered, its value is
    * set to "true". Has no effect on positional parameters.
    *
    * @param repeatPositional If this is a positional parameter, it will be the
    * parser will repeat it indefinitely.
    *
    * @param absorbRemaining Treat all subsequent parameters as positionals,
    *  regardless of their name. This can be useful for constructing nested
    *  commands.
    */
  case class ParamDef(
      names: Seq[String],
      parseAndSet: (String, Option[String]) => ParamResult,
      missing: () => Unit,
      isFlag: Boolean,
      repeatPositional: Boolean,
      absorbRemaining: Boolean
  ) {
    require(names.size > 0, "a parameter must have at least one name")
    require(
      names.head != "--",
      "-- is not a valid parameter name; it is used by the parser to explicitly delimit positional parameters"
    )
    if (names.head.startsWith("-")) {
      require(
        names.forall(_.startsWith("-")),
        "named and positional parameters must not share definitions"
      )
    }
    def isNamed = names.head.startsWith("-")
  }

  sealed trait ParamResult {
    def isAbort = this == Abort
  }
  case object Continue extends ParamResult
  case object Abort extends ParamResult // abort parsing, this stops parsing in its track. Other arguments will not be parsed

  // extractor for named arguments
  private val Named = "(--?[^=]+)(?:=(.*))?".r

  /** Parse command line arguments according to some given parameter definition.
    *
    * The parser works in two passes.
    * 1. the first pass goes over all actual arguments and groups them into
    *    positional and named ones (and also detects any unkown arguments)
    * 2. the second pass then iterates over all parameter definitions, looks up
    *    the corresponding value from the previous pass, and calls the relevant
    *    functions of the parameter defintion
    *
    * Delegating parameter invocation to a second pass allows for them to be
    * evaluated in order of defnition, rather than order of appearance on the
    * command line. This is important to allow "breaking" parameters such as
    * `--help` to be on a command line with other "side-effecting" params, but
    * yet avoid executing part of the command line (of course this example
    * assumes that the `--help` parameter was defined before any others).
    *
    * @param params the sequence of parameter definitions
    * @param args the actual command-line arguments
    * @param reportUnknown a function invoked when an extranous argument is
    * encountered. An extranous argument can be either an unknown named
    * argument, or a superfluous positional argument
    *
    * @return true if no Abort was encountered. Note that this does not necessarily imply failure or success. false otherwise
    */
  def parse(
      params: Seq[ParamDef],
      args: Seq[String],
      reportUnknown: String => Unit
  ): Boolean = {
    val named = mutable.ArrayBuffer.empty[ParamDef] // all named params
    val aliases = mutable.Map.empty[String, ParamDef] // map of all possible names of named params
    val positional = mutable.ArrayBuffer.empty[ParamDef]

    // populate parameter defs
    params.foreach { p =>
      if (p.isNamed) {
        named += p
        p.names.foreach { n => aliases += n -> p }
      } else {
        positional += p
      }
    }

    // parsed arguments
    val namedArgs = mutable.Map.empty[ParamDef, mutable.ListBuffer[
      (String, Option[String]) // name used -> value given (or None if no value given, e.g. flags)
    ]]
    val positionalArgs = mutable.ArrayBuffer.empty[String]
    var pos = 0 // having this as a separate var from positionalArgs.length allows processing repeated params

    // first, iterate over all arguments to detect extraneous ones
    var argIter = args.iterator
    var arg: String = null
    def readArg() = if (argIter.hasNext) arg = argIter.next() else arg = null
    readArg()

    var onlyPositionals = false
    def addPositional(arg: String) =
      if (pos < positional.length) {
        val param = positional(pos)
        positionalArgs += arg
        if (param.absorbRemaining) onlyPositionals = true
        if (!param.repeatPositional) pos += 1
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
          case Named(name, embedded) if aliases.contains(name) =>
            readArg()
            val param = aliases(name)
            namedArgs.getOrElseUpdate(param, mutable.ListBuffer.empty)
            if (embedded != null) { // embedded argument, i.e. one that contains '='
              namedArgs(param) += (name -> Some(embedded))
            } else if (param.isFlag) { // flags never take an arg and are set to "true"
              namedArgs(param) += (name -> Some("true"))
            } else if (arg == null || arg.matches(Named.regex)) { // non-flags may have an arg
              namedArgs(param) += (name -> None)
            } else {
              namedArgs(param) += (name -> Some(arg))
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

    // then, iterate over all parameters to set values or report missing arguments
    for (param <- named) {
      namedArgs.get(param) match {
        case None => param.missing()
        case Some(list) if list.isEmpty =>
          param.missing() // this shouldn't ever happen, but let's be defensive
        case Some(list) =>
          for ((nameUsed, valueOpt) <- list)
            if (param.parseAndSet(nameUsed, valueOpt).isAbort) return false
      }
    }

    for (i <- 0 until pos) {
      if (positional(i).parseAndSet(positional(i).names.head, Some(positionalArgs(i))).isAbort)
        return false
    }
    for (i <- pos until positionalArgs.length) {
      if (positional(pos).parseAndSet(positional(pos).names.head, Some(positionalArgs(i))).isAbort) return false
    }
    for (i <- pos until positional.length) {
      positional(i).missing()
    }

    return true
  }

}
