package argparse

import ArgParser._
import java.io.PrintStream
import java.io.OutputStream

/** Interactive bash completion. (completion logic is generated in advance;
  * requires bash-completion package installed) */
object StandaloneBashCompletion {

  def header(out: PrintStream): Unit = {
    out.println(
      """|# Completion script generated by scala-argparse
         |#
         |# The completion code supports nested commands ('verbs') and is extensible.
         |#
         |# Rules:
         |#
         |# - Completion functions for commands must named by concatenating all commands.
         |#   E.g. if `foo bar baz` represents 3 nested commands, then the completion
         |#   function should be called `_foo_bar_baz`
         |#
         |# - A command completion function for a command should configure its grammar by
         |#   setting the following arrays:
         |#
         |#   - named: named parameters which take an argument
         |#   - flags: named parameters which do not take an argument
         |#   - repeat_pos: the position of a positional parameter which repeats itself
         |#     indefinitely (no other params will be parsed after this)
         |#
         |#   The completion function should then call
         |#   `__<toplevelcommand>_handle_completion`, which will parse the command line
         |#   and generate completions. E.g.
         |#
         |#       _prog() {
         |#         named+=("-n")
         |#         named+=("--name2")
         |#         named+=("--name1")
         |#         flags+=("--flag")
         |#
         |#         __prog_handle_completion
         |#       }
         |#
         |# - Completion functions for parameters must be named after the command and
         |#   parameter.
         |#
         |#   - The completion function for a named parameter must be named
         |#     `_<command>_<name>_`, where <name> includes any dashed. E.g.
         |#     `_foo_bar_baz_--option_`.
         |#   - The completion function for a positional parameter must be named
         |#     `_<command>_<position>_`. E.g. `_foo_bar_baz_0_` for the first positional
         |#     parameter.
         |#
         |#  The completion functions for parameters are responsible for setting the
         |#  COMPREPLY variable. They should use the `$prefix` variable to match input. A
         |#  common pattern of a completion would look something like the following:
         |#
         |#      _prog_0_() {
         |#        COMPREPLY=( $(compgen -W "hello help" -- "$prefix") )
         |#      }
         |#
         |#  In case a completion function is not found, no completions will be generated.
         |
         |# shellcheck shell=bash
         |# shellcheck disable=SC2207
         |""".stripMargin
      )
    }

  /** Generate utility functions and completion entry point.*/
  def utils(out: PrintStream, prog: String): Unit = out.println(
    s"""|# read and populate the next arg
        |__${prog}_next_arg() {
        |    apos=$$((apos+1))
        |    arg="$${words[apos]}"
        |}
        |
        |# word, list*
        |__${prog}_contains_word(){
        |    local w word=$$1; shift
        |    for w in "$$@"; do
        |        [[ $$w = "$$word" ]] && return
        |    done
        |    return 1
        |}
        |
        |
        |__${prog}_handle_completion() {
        |  while [[ $$apos -le $$cword ]]; do
        |    prefix="$$arg"
        |
        |    case "$$arg" in
        |        --)
        |          __${prog}_next_arg ;;
        |        -)
        |          __${prog}_next_arg
        |          if [[ $$ppos -ne $$repeat_pos ]]; then
        |            ppos=$$((ppos+1))
        |          fi
        |          ;;
        |        -*)
        |          local prev=$$arg
        |          local name=$$arg
        |          if [[ $$arg == *=* ]]; then
        |            name=$${arg%%=*}
        |          fi
        |
        |          __${prog}_next_arg
        |          if __${prog}_contains_word "$$name" "$${named[@]}"; then
        |            if [[ $$prev == *=* ]]; then
        |              completer_fn="$${current_fn}_$${name}_"
        |              prefix=$${prev#*=} # embedded argument after '='
        |            elif [[ $$apos -le $$cword ]]; then
        |              completer_fn="$${current_fn}_$${name}_"
        |              prefix="$$arg"
        |              __${prog}_next_arg
        |            fi
        |          fi
        |          ;;
        |        *)
        |          if [[ $$ppos -ne $$repeat_pos ]]; then
        |            ppos=$$((ppos+1));
        |          fi
        |          # if a function to handle a subcommand exists, then call it
        |          if  declare -F "$${current_fn}_$${arg}" > /dev/null && [[ $$apos -lt $$cword ]]; then
        |            current_fn=$${current_fn}_$${arg}
        |            __${prog}_next_arg
        |
        |            # reset parser state before calling nested command
        |            ppos=-1
        |            named=()
        |            flags=()
        |            prefix=""
        |            $$current_fn
        |            return
        |          else
        |            completer_fn="$${current_fn}_$${ppos}_"
        |            __${prog}_next_arg
        |          fi
        |          ;;
        |    esac
        |  done
        |
        |  if [[ $$prefix == -* ]]; then
        |    COMPREPLY=( $$(compgen -S '=' -W "$${named[*]}" -- "$$prefix") $$(compgen -W "$${flags[*]}" -- "$$prefix") )
        |  elif declare -F "$$completer_fn" > /dev/null; then
        |    "$$completer_fn"
        |  fi
        |  [[ "$${COMPREPLY[0]}" == *= ]] && compopt -o nospace
        |}
        |
        |__${prog}_start() {
        |    # shellcheck disable=SC2034
        |    local cur prev words cword # (cur and prev are not used)
        |    _get_comp_words_by_ref -n = cur prev words cword
        |
        |    local apos=0 # current argument position
        |    local arg="" # current argument
        |
        |    local ppos=-1 # current positional parameter
        |    local named=() # named params which take a value
        |    local flags=() # named params which do not take a value
        |    local repeat_pos="" # index of a positional parameter which will repeat indefinitely
        |    local prefix="" # string against which completions will be matched
        |
        |    local completer_fn="" # function invoked for completion; should set COMPREPLY
        |    local current_fn="_${prog}" # current function to call in nested commaned
        |
        |    __${prog}_next_arg
        |    _${prog}
        |}
        |
        |complete -F __${prog}_start ${prog}
        |""".stripMargin
  )

  /** Generate completion funtion for parameters. All parameters of a command
    * must be given at once, since the order of positionals affects the name.
    */
  def parameters(
    out: PrintStream,
    prog: String,
    subcommandChain: Seq[String],
    paramInfos: Seq[ParamInfo]
  ): Unit = {
    def parameter(paramName: String, info: ParamInfo): Unit = {
      out.print(s"_${prog}")
      for (part <- subcommandChain) {
        out.print("_")
        out.print(part)
      }
      out.print("_")
      out.print(paramName)
      out.print("_")
      out.println("(){")

      info.bashCompleter match {
        case Reader.BashCompleter.Empty => // should not happen
        case Reader.BashCompleter.Fixed(words) =>
          out.println(
            s"""  COMPREPLY=( $$(compgen -W "${words.mkString(" ")}" -- "$$prefix") )"""
          )
        case Reader.BashCompleter.Default =>
          out.println(
            s"""  compopt -o default"""
          )
      }
      out.println("}")
    }

    val (namedParams, positionalParams) = paramInfos.partition(_.isNamed)
    for (param <- namedParams) {
      if (param.bashCompleter != Reader.BashCompleter.Empty) {
        for (alias <- param.names) {
          parameter(alias, param)
        }
      }
    }
    for ((param, idx) <- positionalParams.zipWithIndex) {
      if (param.bashCompleter != Reader.BashCompleter.Empty) {
        parameter(idx.toString, param)
      }
    }
  }

  /** Generate completion funtion for a command. */
  def command(
    out: PrintStream,
    prog: String, // top level command
    subcommandChain: Seq[String], // sub commands leading to this command
    paramInfos: Seq[ParamInfo]
  ): Unit = {
    out.print(s"_${prog}")
    for (part <- subcommandChain) {
      out.print("_")
      out.print(part)
    }
    out.println("(){")

    var pos: Int = 0
    for (param <- paramInfos) {
      if (param.isNamed) {
        if (param.isFlag) {
          for (name <- param.names) {
            out.println(s"  flags+=($name)")
          }
        } else {
          for (name <- param.names) {
            out.println(s"  named+=($name)")
          }
        }
      } else {
        pos += 1
        if (param.repeats) {
          out.println(s"  repeat_pos=${pos-1}")
        }
      }
    }
    out.println(s"  __${prog}_handle_completion")
    out.println("}")
  }

  /** Generate completion code for a whole command line application, recursively
    * invoking nested commands. */
  def all(
    out: PrintStream,
    fullCommand: String,
    paramInfos: Seq[ParamInfo],
    commandInfos: Seq[CommandInfo]
  ): Unit = {
    val parts = fullCommand.split(" ").toIndexedSeq
    require(parts.length > 0, "the full command name must not be empty")

    // common completion code for all cases
    def generateCompletion() = {
      for (command <- commandInfos) {
        // magic command
        try {
          command.action(Seq("---nested-completion", s"$fullCommand ${command.name}"))
        } catch {
          case _: CompletionReturned =>
        }
      }
      parameters(out, parts.head, parts.tail, paramInfos)
      command(out, parts.head, parts.tail, paramInfos)
    }

    if (parts.length == 1) { // top level completion request
      header(out)
      generateCompletion()
      utils(out, fullCommand)
    } else { // sub-command completion request
      generateCompletion()
    }
  }

   def completeAndThrow(
      out: java.io.PrintStream,
      paramInfos: Seq[ParamInfo],
      commandInfos: Seq[CommandInfo],
      args: Seq[String]
  ): Unit = {
    // Since argparse supports independent nested commands, we must have a way
    // to recursively request completion from nested commands. We use the
    // 'magic' parameter "---nested-completion" to achieve this. If that
    // argument is encountered, then it is assumed that we are generating a
    // completion script.
    if (args.length == 2 && args.head == "---nested-completion") {
      all(out, args(1), paramInfos, commandInfos)
      throw CompletionReturned()
    }
  }

  case class CompletionReturned() extends Throwable("returning after completion generated", null, true, false)

}
