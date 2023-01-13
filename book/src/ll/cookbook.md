## Cookbook

This *cookbook* contains recipes (i.e. "how-tos") for accomplishing common tasks
which are more advanced than what is described in the [Tutorial](#tutorial).

### Subcommands

Many applications actually split their functionality into multiple nested
commands, each corresponding to the verb of an action (such as `docker run` or
`git clone`). This approach works particularly well if an application performs
different functions which require different kinds of arguments.

`ArgumentParser` has built-in support for these kinds of sub-commands with the
`subparser()` method. This method will return a new `ArgumentParser` which can
be modified as usual. The parent parser is aware of the child parser, and will
include it in help messages and bash completion scripts. Each child parser will
have its own parameters, but can access the arguments declared in the parent.

```scala
{{#include ../../../examples/subparsers/src/main.scala}}
```

```
{{#include ../../../examples/subparsers/src/shell.txt}}
```

### Bash Completion

If you are an avid user of the command line, you will probably have noticed that
you can get argument suggestions by pressing the `tab` key on a partially typed
word. This is a very helpful feature for quickly navigating and exploring
command line tools. It is known as *bash completion*, and can work in one of two
ways:

1. Standalone. A *completion script* has been *sourced* by your shell and is
   what is called to generate completions when you press tab.

2. Interactive. Your program is called to complete the partially typed word.

The `argparse` library allows you to use both options with minimal setup. We do
however recommend to use *standalone* completion if you are writing your program
for the JVM, since you otherwise have to suffer the JVM's startup delay when
you're waiting for tab completion.

#### Standalone Bash Completion

Every `ArgumentParser` accepts a `--bash-completion` parameter which will
generate a bash-completion script. You can source this script at the start of
your shell session, for example by adding it to your `~/.bashrc`.

Example:

```scala
{{#include ../../../examples/completion1/src/main.scala}}
```

```
$ app --bash-completion app > complete.sh
$ source complete.sh
$ app -[press tab]
--bar=  --baz=  --foo=  --help
$ app --baz=[press tab]
a  b
```

How it works:

- The argument `--bash-completion` will generate a completion script for bash.

  You can read the full details of how bash completion works on `man 1 bash`.

- Completions are based on the parameter type, but can be overriden by
  explicitly setting the `standaloneCompleter` parameter.

- Completion will only complete positional arguments by default, unless you have
  started typing a word which starts with `-`.

- This works also with subcommands.

We suggest that you include the bash completion script in the files distributed alongside the binary distribution of your application.

#### Interactive Bash Completion

You can also write completion logic in the program itself. In this case, you
will need to instruct bash to call your program with a special environment when
you press tab. You can do this by running `complete -o nospace -C <program>
<program>` at the start of your shell session, for exmaple by putting it into
`~/.bashrc`.

Example:

```scala
{{#include ../../../examples/completion2/src/main.scala}}
```

```
$ complete -o nospace -C app app
$ app -[press tab]
--bar    --baz    --foo    --help
$ app --baz [press tab]
a  b
```

How it works:

- Bash sets a few "magic" environment variables before invoking your program,
  which will cause it to behave differently than when invoked normally.

- Completions are based on the parameter type, but can be overriden by
  explicitly setting the `interactiveCompleter` parameter.

- You can read the full details of how bash completion works on `man 1 bash`.

We suggest that you only use interactive completion for programs targeting
Scala Native.

### Depending on another Parameter

In some situations you may want a parameter's default value to depend on the
value of another parameter. You can achieve this by simply calling the
argument holder's `value` method in the default.

Example:

```scala
{{#include ../../../examples/paramdep/src/main.scala}}
```

```
{{#include ../../../examples/paramdep/src/shell.txt}}
```

How it works:

- The `default` method parameter is call-by-name.
- Arguments are parsed in order of parameter definition. Hence a parameter can
  reference the values of others in its default value.

### Adding Support for a New Type of Parameter

This library has support for reading arguments for many kinds of Scala types. In
advanced programs however, it can happen that you run into an unsupported type.
You will receive a *compile-time* error, informing you that a specific type is
not supported. In this situation, you can define a custom *API bundle* with an
additional `Reader` for your type of parameter.

Example of the problem:

```scala
case class Level(n: Int)

def main(args: Array[Strig]): Unit =
  val parser = argparse.default.ArgumentParser()
  val level = parser.requiredParam[Level]("log-level") // Compile error: no Reader[Level] found
  parser.parseOrExit(args)
  println(bytes.value.n)
```

Solution:

```scala
{{#include ../../../examples/reader/src/main.scala}}
```

```scala
{{#include ../../../examples/reader/src/shell.txt}}
```

How it works:

- `Reader` is a *typeclass* which is responsible for parsing strings from the
  command line into instances of Scala types.

- Readers are declared in an API bundle. An API bundle is a bunch of traits that
  are mixed together in order to define "a flavor" of argparse.

  The default bundle implemented in this library is
  [`argparse.default`](../javadoc/api/argparse/default$.html), which includes
  Readers for most common types.

- You can create a custom bundle by creating an object which extends
  `argparse.core.Api`, and declare additional readers in it.

- The `ArgumentParser` from the custom bundle will find the *Reader* instance
  which can parse the desired parameter type.

