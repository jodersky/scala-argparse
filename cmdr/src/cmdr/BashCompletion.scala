package cmdr

import collection.mutable

import ArgumentParser.ParamInfo
import ArgumentParser.CommandInfo

object BashCompletion {

  private val Named = "(--?[^=]+)(?:=(.*))?".r

  /** Check if tab completion is requested and print completions.
    *
    * Returns false if no completions were requested. This function assumes that
    * it has been called in an environment set by `complete -C`.
    *
    * See `man bash` (search for Programmable Completion) for more information.
    */
  def completeOrFalse(
      paramInfos: Seq[ParamInfo],
      commandInfos: Seq[CommandInfo],
      env: Map[String, String],
      args: Seq[String],
      stdout: java.io.PrintStream
  ): Boolean = {
    val comppoint = env.get("COMP_POINT")
    val compline = env.get("COMP_LINE")

    // Since cmdr supports independent nested commands, we must have a way to
    // consume partial arguments and feed the rest to nested commands. We use
    // the 'magic' parameter "--recursive-complete" to achieve this. If that
    // argument is encountered, then it is assumed that we are completing a
    // command line, and that all remaining arguments represent the command line
    // to be completed. The initial command line is extracted from bash's
    // COMP_LINE environment variable.
    if (args.length > 0 && args.head == "--recursive-complete") {
      // nested completion
      complete(paramInfos, commandInfos, args.tail, stdout)
      true
    } else if (comppoint.isDefined && compline.isDefined) {
      // top-level completion
      val length = comppoint.get.toInt

      // Notes:
      // - the '-1' makes sure that 'split' will include trailing spaces as
      //   empty strings. This is necessary to distinguish completion requests
      //   between a partially typed word and an empty new word.
      //   E.g.
      //     "a b c".split("\\s+", -1) == Array("a", "b", "c")
      //     "a b c ".split("\\s+", -1) == Array("a", "b", "c", "")
      // - .tail is safe, because COMP_LINE will always include $0
      val words = compline.get.take(length).split("\\s+", -1).tail
      completeOrFalse(
        paramInfos,
        commandInfos,
        env,
        Seq("--recursive-complete") ++ words,
        stdout
      )
    } else {
      // no completion requested
      false
    }
  }

  private def complete(
      paramInfos: Seq[ParamInfo],
      commandInfos: Seq[CommandInfo],
      args: Seq[String],
      stdout: java.io.PrintStream
  ): Unit = {
    val named = paramInfos.filter(_.isNamed)
    val positionals = paramInfos.filter(!_.isNamed)

    val argIter = args.iterator
    var arg: String = null
    def readArg() = if (argIter.hasNext) arg = argIter.next() else arg = null
    readArg()

    val positionalCompleters: Iterator[String => Seq[String]] =
      positionals.map(_.completer).iterator

    var completer: ArgParser.Completer = ArgParser.NoCompleter
    var prefix: String = ""

    // first, iterate over all arguments and:
    // - set special completers for named parameters
    // - follow nested commands
    // - set special completers for positional parameters
    // TODO: handle '--' positional escaping
    while (arg != null) {
      prefix = arg

      arg match {
        case Named(name, embedded) if named.exists(_.names.contains(name)) =>
          val param = named.find(_.names.contains(name)).get
          readArg()

          if (embedded != null) {
            completer = param.completer
            prefix = embedded
          } else if (param.isFlag) {
            // no special completion for flags
          } else if (arg == null) {
            // at the end there is nothing to do
          } else {
            // not at the end
            completer = param.completer
            prefix = arg
            readArg()
          }
        case Named(_, _) =>
          readArg()
        case positional =>
          readArg()

          if (commandInfos.exists(_.name == positional) && arg != null) {
            return commandInfos
              .find(_.name == positional)
              .get
              .action(
                Seq("--recursive-complete", arg) ++ argIter.toSeq
              )
          } else {
            completer =
              positionalCompleters.nextOption().getOrElse(ArgParser.NoCompleter)
          }
      }
    }

    // then, do the actual completion, depending on what completer was found in
    // the first step
    if (prefix.startsWith("-")) { // '-' will show all available named params
      named.flatMap(_.names).filter(_.startsWith(prefix)).foreach { f =>
        stdout.print(f)
        stdout.println(" ")
      }
    } else {
      completer(prefix).foreach(stdout.println)
    }
  }

}
