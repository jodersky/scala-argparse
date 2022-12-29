package argparse.core

import collection.mutable

/** Completion logic that is handled by this, scala, program. */
object InteractiveBashCompletion {

  private val Named = "(--?[^=]+)(?:=(.*))?".r

  /** Check if tab completion is requested and print completions.
    *
    * Returns false if no completions were requested. This function assumes that
    * it has been called in an environment set by `complete -C`.
    *
    * See `man bash` (search for Programmable Completion) for more information.
    */
  def completeOrFalse(
    parser: ParsersApi#ArgumentParser,
    env: Map[String, String],
    stdout: java.io.PrintStream
  ): Boolean = {
    val comppoint = env.get("COMP_POINT")
    val compline = env.get("COMP_LINE")
    if (comppoint.isDefined && compline.isDefined) {
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
      complete(parser, words, stdout)
      true
    } else {
      // no completion requested
      false
    }
  }

  private def complete(
      parser: ParsersApi#ArgumentParser,
      args: Iterable[String],
      stdout: java.io.PrintStream
  ): Unit = {
    val named = parser.paramInfos.filter(_.isNamed)
    val positionals = parser.paramInfos.filter(!_.isNamed)

    val argIter = args.iterator
    var arg: String = null
    def readArg() = if (argIter.hasNext) arg = argIter.next() else arg = null
    readArg()

    val positionalCompleters: Iterator[String => Seq[String]] =
      positionals.map(_.interactiveCompleter).iterator

    var completer: String => Seq[String] = _ => Seq.empty
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
            completer = param.interactiveCompleter
            prefix = embedded
          } else if (param.isFlag) {
            // no special completion for flags
          } else if (arg == null) {
            // at the end there is nothing to do
          } else {
            // not at the end
            completer = param.interactiveCompleter
            prefix = arg
            readArg()
          }
        case Named(_, _) =>
          readArg()
        case positional =>
          readArg()

          parser.subparsers.get(positional) match {
            case Some(subparser) if arg != null =>
              return complete(subparser, Seq(arg) ++ argIter.toSeq, stdout)
            case _ =>
              completer =
                positionalCompleters.nextOption().getOrElse(_ => Seq.empty)
          }
      }
    }

    // then, do the actual completion, depending on what completer was found in
    // the first step
    if (prefix.startsWith("-")) { // '-' will show all available named params
      named.flatMap(_.names).filter(_.startsWith(prefix)).sorted.foreach { f =>
        stdout.print(f)
        stdout.println(" ")
      }
    } else {
      completer(prefix).foreach(stdout.println)
    }
  }

}
