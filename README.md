- [scala-argparse ![Latest version](#scala-argparse-latest-versionscaladex-link-gitter-chatgitter-badgegitter-link)
  - [Highlights](#highlights)
  - [Example](#example)
  - [Usage](#usage)
  - [Tutorial](#tutorial)
    - [Concepts](#concepts)
    - [Basics](#basics)
    - [Introducing Required Parameters](#introducing-required-parameters)
    - [Introducing Optional Parameters](#introducing-optional-parameters)
    - [Introducing Repeated Parameters](#introducing-repeated-parameters)
    - [Named Parameters and Positional Parameters](#named-parameters-and-positional-parameters)
    - [Flags](#flags)
    - [Short Named Parameters and Aliases](#short-named-parameters-and-aliases)
    - [Reading from the Environment](#reading-from-the-environment)
    - [Bash Completion](#bash-completion)
  - [Cookbook](#cookbook)
    - [Subcommands](#subcommands)
    - [Bash Completion](#bash-completion-1)
      - [Standalone Bash Completion](#standalone-bash-completion)
      - [Interactive Bash Completion](#interactive-bash-completion)
    - [Depending on Another Parameter](#depending-on-another-parameter)
    - [Customizing Parsing](#customizing-parsing)
      - [Adding a New Type of Parameter](#adding-a-new-type-of-parameter)
      - [Overriding Help Message](#overriding-help-message)
    - [Writing Man Pages](#writing-man-pages)
  - [Changelog](#changelog)
    - [0.15.2](#0152)
    - [0.15.1](#0151)
    - [0.15.0](#0150)
    - [0.14.0](#0140)
    - [0.13.0](#0130)
    - [0.12.1](#0121)
    - [0.12.0](#0120)
    - [0.11.0](#0110)
    - [0.10.3](#0103)
    - [0.10.2](#0102)
    - [0.10.1](#0101)
    - [0.10.0](#0100)
    - [0.9.0](#090)
    - [0.8.0 and before](#080-and-before)
  - [Glossary](#glossary)
  -
# scala-argparse [![Latest version][scaladex-badge]][scaladex-link] [![Gitter Chat][gitter-badge]][gitter-link]

[scaladex-badge]: https://index.scala-lang.org/jodersky/scala-argparse/argparse/latest-by-scala-version.svg
[scaladex-link]: https://index.scala-lang.org/jodersky/scala-argparse/argparse
[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-link]: https://gitter.im/jodersky/scala-argparse

Pragmatic command line parsing for Scala applications.

## Highlights

- Simple interface, inspired by the
  [argparse](https://docs.python.org/3/library/argparse.html) package from
  python.

- Bash completion.

  - Standalone bash completion for a super snappy user experience, even on the
    JVM.

  - Interactive bash completion for the most custom needs. `complete -o nospace -C <program>
  <program>` to `~/.bashrc`

## Example

<!--example 1-->
```scala
package example

def main(args: Array[String]): Unit = {
  val parser = argparse.default.ArgumentParser(description = "an example application")

  // a named parameter
  val host = parser.param[String](
    name = "--host",
    default = "localhost",
    help = "the name of the host"
  )

  // a named parameter which accepts only integers
  val port = parser.param[Int](
    name = "--port",
    default = 8080,
    aliases = Seq("-p"),
    env = "PORT",
    help = "some port"
  )

  // a named parameter which does not take an argument, aka a "flag"
  val secure = parser.param[Boolean](
    name = "--secure",
    default = false,
    flag = true
  )

  // a positional parameter which accepts only paths
  val path = parser.requiredParam[java.nio.file.Path](
    "path",
    help = "the path to use"
  )

  parser.parseOrExit(args)

  val scheme = if (secure.value) "https" else "http"
  val url = s"$scheme://${host.value}:${port.value}/${path.value}"
  println(url)
}
```
<!--/example-->

Run it:

```
$ ./mill examples.readme
missing argument: path
run with '--help' for more information
```

```
$ ./mill examples.readme a
http://localhost:80/a
```

```
$ ./mill examples.readme a/b/c --host 1.1.1.1 -p 10 --secure
https://1.1.1.1:10/a/b/c
```

```
$ PORT=80 ./mill examples.readme a
http://localhost:80/a
```

```
$ ./mill examples.readme --help
usage: [options] <path>

an example application

positional arguments:
  path
        the path to use
named arguments:
  --host=<string>
        the name of the host
  --port=, -p=<int>
        some port
  --secure

environment variables:
  PORT                           sets --port
```

## Usage

This library is published for Scala 3 and 2.13, for the JVM and Native. It is
available on maven central under the coordinates:

- mill: `ivy"io.crashbox::argparse::<version>"`
- sbt: `"io.crashbox" %%% "argparse" % "<version>"`

where `<version>` is given by [![Latest
version](https://index.scala-lang.org/jodersky/scala-argparse/argparse/latest.svg)](https://index.scala-lang.org/jodersky/scala-argparse/argparse)

## Tutorial

*Acknowledgement: this tutorial is inspired by and largely copied from the
[Python Argparse Tutorial by Tshepang
Lekhonkhobe](https://docs.python.org/3/howto/argparse.html), made available
under the Zero Clause BSD License.*

This tutorial is intended to be a gentle introduction to argparse, my
recommended command-line parsing library for Scala.

### Concepts

Let's show the sort of functionality that we are going to explore in this
introductory tutorial by making use of the `ls` command:

```
$ ls
argparse  build.sc  ci  examples  ini  LICENSE.md  mill  out  README.md
$ ls argparse
src  src-2  src-3  test
$ ls -l
total 48
drwxr-xr-x 9 jodersky jodersky  4096 Jul  3 10:12 argparse
-rw-r--r-- 1 jodersky jodersky  4836 Jul  3 15:30 build.sc
drwxr-xr-x 2 jodersky jodersky  4096 Jan 31 18:05 ci
drwxr-xr-x 5 jodersky jodersky  4096 Jul  3 15:07 examples
drwxr-xr-x 4 jodersky jodersky  4096 May 20 19:58 ini
-rw-r--r-- 1 jodersky jodersky  1473 Apr 30  2020 LICENSE.md
-rwxr-xr-x 1 jodersky jodersky  1646 Mar  7  2021 mill
drwxr-xr-x 5 jodersky jodersky  4096 Jul  3 15:54 out
-rw-r--r-- 1 jodersky jodersky 10090 Jul  3 17:10 README.md
$ ls --help
Usage: ls [OPTION]... [FILE]...
List information about the FILEs (the current directory by default).
Sort entries alphabetically if none of -cftuvSUX nor --sort is specified.
...
```

A few concepts we can learn from the four commands:

- The **ls** command is useful when run without any arguments at all. It
  defaults to displaying the contents of the current directory.

- If we want beyond what it provides by default, we tell it a bit more. In this
  case, we want it to display a different directory, `argparse`. What we did is
  specify what is known as a *positional argument*. It's called so because the
  program should know what to do with the value, solely based on where it
  appears on the command line. This concept is more relevant to a command like
  **cp**, whose most basic usage is `cp SRC DEST`. The first position is what
  you want copied, and the second position is where you want it copied to.

- Now, say we want to change behaviour of the program. In our example, we
  display more info for each file instead of just showing the file names. The
  `-l` in that case is known as a *named argument*.

- That's a snippet of the help text. It's very useful in that you can come
  across a program you have never used before, and can figure out how it works
  simply by reading its help text.


These concepts are core to `argparse`:

**parameter**
: a named value in a command line

**argument**
: the value assigned to a parameter

***named* argument**
: an argument that starts with `-`. The characters following determine the name
of the parameter that the argument is assigned to. The actual value assigned to
the parameter is given after an '=' or a space. For instance `--foo=bar` assigns
`bar` to `foo`. Named arguments may appear in any order on a command line.

***positional* argument**
: an argument that is not named. Positional arguments are assigned to positional
parameters according to their respective order of occurence.

### Basics

Let's start with an example that does almost nothing:

```scala
def main(args: Array[String]): Unit =
  val parser = argparse.ArgumentParser()
  parser.parseOrExit(args)
```

The following is a result of running the code:

```
$ app
$ app --help
TODO
$ app --verbose
TODO
$ app foo
TODO
```

Here is what is happening:

- Running the program without any arguments results in nothing displayed to
  stdout. Not so useful.

- The second one starts to display the usefulness of the `argparse` library. We
  have done almost nothing, but already we get a nice help message.

- The `--help` option is the only option we get for free (i.e. no need to
  specify it). Specifying anything else results in an error. But even then, we
  do get a useful usage message, also for free.

### Introducing Required Parameters

An example:

```scala
def main(args: Array[Strig]): Unit =
  val parser = argparse.default.ArgumentParser()
  val echo = parser.requiredParam[String]("echo")
  parser.parseOrExit(args)
  print(echo.value)
```

And running the code:

```
$ python3 prog.py
usage: prog.py [-h] echo
prog.py: error: the following arguments are required: echo
$ python3 prog.py --help
usage: prog.py [-h] echo

positional arguments:
  echo

options:
  -h, --help  show this help message and exit
$ python3 prog.py foo
foo
```

Here is what's happening:

- We've added the `requiredParam()` method, which is what we use to specify
  which command-line arguments the program needs. In this case, I've named it
  `echo` so that it's in line with its function.

  The result of this method is a holder to some future value of an argument,
  which can be accessed by calling the `.value` method.

- Calling our program now **requires** us to specify an argument.

- We can specify how an argument should be read from the command line by
  specifying the type of the parameter, `String` in this case.

- The `parser.parseOrExit()` method is what actually goes through the command
  line arguments and sets the argument holders' values.

  After calling this method, the arguments can be accessed via the `.value`
  method of the argument holders.

Note however that, although the help display looks nice and all, it currently is
not as helpful as it can be. For example we see that we got `echo` as a
positional argument, but we don't know what it does, other than by guessing or
by reading the source code. So, let's make it a bit more useful:

```scala
def main(args: Array[Strig]): Unit =
  val parser = argparse.default.ArgumentParser()
  val echo: argparse.Argument[String] = parser.requiredParam[String](
    "echo",
    help="echo the string you use here"
  )
  parser.parseOrExit(args)
  print(echo.value)
```

And we get:

```
TODO
```

Now, how about doing something even more useful:

```scala
def main(args: Array[Strig]): Unit =
  val parser = argparse.default.ArgumentParser()
  val square = parser.requiredParam[Int](
    "square",
    help="display a square of a given number"
  )
  parser.parseOrExit(args)
  print(square.value * square.value)
```

Following is a result of running the code:

```
$ python3 prog.py 4
16
$ python3 prog.py four
usage: prog.py [-h] square
prog.py: error: argument square: invalid int value: 'four'
```

That went well. The program now even helpfully quits on bad input before
proceeding.

### Introducing Optional Parameters

So far the parameters that we have specified were required. Let's look at how we
can make an argument optional.

An example:

```scala
def main(args: Array[Strig]): Unit =
  val parser = argparse.default.ArgumentParser()
  val answer = parser.param[Int](
    "answer",
    default = 42,
    help="display the answer to the universe"
  )
  parser.parseOrExit(args)
  print(answer.value)
```

Following is a result of running the code:

```
$ app
$ app 1000
```

Here is what's happening:

- The parameter is made **optional** by declaring it with the `param()` method
  instead of the `requiredParam()` method.

- This method requires a default value.

- The default value will be used if the argument is not encountered on the
  command-line.

### Introducing Repeated Parameters

An example:

```scala
def main(args: Array[Strig]): Unit =
  val parser = argparse.default.ArgumentParser()
  val files = parser.repeatedParam[java.nio.file.Path](
    "files",
    help="remove these files"
  )
  parser.parseOrExit(args)
  for (file <- files.value) {
    println(s"if this were a real program, we would delete $file")
  }
```

Following is a result of running the code:

```
$ app
$ app file1
$ app file1 file2 file3
```

Here is what's happening:

- The parameter is made **repeated** by declaring it with the `repeatedParam()`.

- Repeated parameters accumulate all ocurences in the argument holder.

  Thus, `value` will give us back a `Seq[java.nio.file.Path]` rather than a
  `java.nio.file.Path` in this case.

### Named Parameters and Positional Parameters

So far you may have noticed that all our examples have used *positional*
parameters. Recall from the initial `ls` example, that a positional parameter is
one which is set solely based on the position of its argument. When parameter
lists get very long or change over time, it can become very difficult to keep
matching argument lists coherent. Therefore, most command line tools will mostly
use *named* parameters.

An example:

```scala
def main(args: Array[Strig]): Unit =
  val parser = argparse.default.ArgumentParser()
  val verbosity = parser.param[Int](
    "--verbosity",
    default = 0
    help = "level of verbosity"
  )
  val files = parser.repeatedParam[java.nio.file.Path](
    "files",
    help="remove these files"
  )
  parser.parseOrExit(args)
  for (file <- files.value) {
    if (verbosity.value > 0) {
      println(s"deleting $file")
    }
    println(s"if this were a real program, we would delete $file")
  }
```

Following is a result of running the code:

```
$ app --verbosity
$ app --verbosity 1
$ app --verbosity=1
$ app --verbosity 1 file1 file2
$ app file1 --verbosity 1 file2
$ app file1 -- --verbosity 1 file2
```

Here is what is happening:

- A *named* parameter is a parameter that is identified by a name on the command
  line instead of a position. Named arguments are syntactically distinguished
  from positional arguments by a leading `-` (or `--` as is common).

- Arguments to named parameters can be either separated by a space or an equals
  sign.

- Named arguments may appear in any order on the command line. They can be
  intermingled with positional arguments.

- A standalone `--` serves as a delimiter, and allows arguments that start with
  `-` to be treated as positionals. This is very handy for accepting untrusted
  input in scripts, or deleting files that start with a hyphen.

All parameter declaration methods, `param()`, `requiredParam()` and
`repeatedParam()` allow defining parameters as positional and named. The only
hint you can see is in the name: if it starts with '-', then it is a named
parameter, otherwise it is positional. Note however, that it is most common and
good practice to only use positional parameters for required parameters, or
conversely, always make named parameters optional.

### Flags

Flags are a special kind of named parameter. They don't take an argument, and we
are only ever interested if they are present on the command-line or not.

An example:

```scala
def main(args: Array[Strig]): Unit =
  val parser = argparse.default.ArgumentParser()
  val verbose = parser.param[Boolean](
    "--verbose",
    default = false
    help = "use verbose output",
    flag = true
  )
  parser.parseOrExit(args)
  println(verbose.value)
```

Running it:

```
$ app
$ app --verbose
$ app --verbose=false
```

How it works:

- A parameter is declared as a **flag** by setting the *flag* parameter,

  This instructs the argument parser that the parameter does not take an
  argument.

- If the flag is encountered on the command line, then it is assigned the string
  value `"true"`.

  Thus, it only makes sense to declare `Boolean` parameters as `flags`.

- You can still override the argument by explicitly passing it after an equals
  sign.

### Short Named Parameters and Aliases

If you are familiar with command line usage, you will notice that I haven't yet
touched on the topic of short versions of named parameters. It's quite simple:

```scala
def main(args: Array[Strig]): Unit =
  val parser = argparse.ArgumentParser()
  val verbose = parser.param[Boolean](
    "--verbose",
    default = false,
    aliases = Seq("-v", "--talkative"),
    flag = true,
    help="use verbose output"
  )
  parser.parseOrExit(args)
  println(verbose.value)
```

And here goes:

```
$ python3 prog.py -v
verbosity turned on
$ python3 prog.py --help
usage: prog.py [-h] [-v]

options:
  -h, --help     show this help message and exit
  -v, --verbose  increase output verbosity
```

Note that the new aliases are also reflected in the help text.

### Reading from the Environment

In some cases it can be useful to fall back to reading a command line from an
environment variable.

Example:

```scala
def main(args: Array[Strig]): Unit =
  val parser = argparse.ArgumentParser()
  val creds = parser.param[os.Path](
    "--credentials-file",
    default = os.home / ".app" / "creds",
    env = "APP_CREDENTIALS_FILE"
    help="the file containing service credentials"
  )
  parser.parseOrExit(args)
  println(creds.value)
```

Run it:

```
$ APP_CREDENTIALS_FILE=/etc/foo app
$ APP_CREDENTIALS_FILE=/etc/foo2 app --creds=/etc/foo1
$ app
```

How it works:

- Required and optional parameter declarations can specify an `env`, which will
  name an environment variable to use if the argument cannot be found on the
  command line.

- The order of precedence is:
  1. the argument on the command line
  2. the environment variable
  3. the default value

### Bash Completion

---

## Cookbook

This *cookbook* contains recipes (i.e. "how-tos") for accomplishing common tasks
which are more advanced than what is described in the [Tutorial](#tutorial).

### Subcommands

TODO

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
def main(args: Array[Strig]): Unit =
  val parser = argparse.ArgumentParser()
  parser.param[os.Path]("--foo", os.pwd)
  parser.param[os.Path]("--bar", os.pwd)
  parser.param[os.Path](
    "--baz",
    os.pwd,
    standaloneCompleter = argparse.BashCompleter.Fixed(Set("a", "b"))
  )
  parser.parseOrExit(args)
```

```
$ app --bash-completion app > complete.sh
$ source complete.sh
$ app -[press tab]
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

You can also write completion logic in your program itself. In this case, you
will need to instruct bash to call your program with a special environment when
you press tab. You can do this, by running `complete -o nospace -C <program>
<program>` at the start of your shell session, for exmaple by putting it into
`~/.bashrc`.

Example:

```scala
def main(args: Array[Strig]): Unit =
  val parser = argparse.ArgumentParser()
  parser.param[os.Path]("--foo", os.pwd)
  parser.param[os.Path]("--bar", os.pwd)
  parser.param[os.Path](
    "--baz",
    os.pwd,
    interactive
    interactiveCompleter = s => Seq("a", "b"))
  )
  parser.parseOrExit(args)
```

```
$ complete -o nospace -C app app
$ app -[press tab]
```

How it works:

- Bash sets a few "magic" environment variables before invoking your program,
  which will cause it to behave differently than when invoked normally.

- Completions are based on the parameter type, but can be overriden by
  explicitly setting the `interactiveCompleter` parameter.

- You can read the full details of how bash completion works on `man 1 bash`.

We suggest that you only use interactive completion for programs targeting
Scala Native.

### Depending on Another Parameter

### Customizing Parsing

#### Adding a New Type of Parameter

#### Overriding Help Message

### Writing Man Pages


Look at [the API docs](https://jodersky.github.io/scala-argparse/api/argparse/ArgumentParser.html)
([defined here](argparse/src/argparse/ArgumentParser.scala)) for parsing rules and
explanations on how it works.

## Changelog

### 0.15.2

- Add an INI printer
- Derive publish version from Git

### 0.15.1

- Cleanup INI parser support and add more tests.

### 0.15.0

- Add support for Scala Native for Scala 3.
- Add standalone bash completion. This allows user programs to generate bash
  scripts for completion, rather than relying on the program itself to generate
  completions. Although the former is less powerful than the latter, it is
  suitable for JVM programs, where the startup cost would be prohibitive for
  interactive completions.
- Remove the ability for parameters to fall back to values provided in a
  configuration file. This was experimental, and configuration files are not in
  scope of this project.
- Move INI-style configuration parser into separate package.
- Add toggles for default help and bash-completion parameters.

### 0.14.0

Rename project to scala-argparse.

### 0.13.0

- Remove ability to read args from a file (predef reader). This caused ordering
  issues in the parser and seemed like a recipe for obsuring config origins.
- Implement an ini-style config parser, and allow params to read from config.

### 0.12.1

- Add range reader.
- Upgrade mill to 0.9.10

### 0.12.0

- Add experimental case class parser (Scala 3 only), available under
  `parser.settings` (the previous mutable settings parser has been renamed to
  `parser.mutableSettings`).
- Add readers for:
  - `scala.concurrent.time.Duration`
  - Common `java.time` data types
  - Collections of paths. These readers use `:` as a separator, instead of the
    usual `,`'.
- Upgrade to Scala 3.0.2.

### 0.11.0

- Upgrade to Scala 2.13.6
- Refactor XDG directory implemention
- Refactor default help message

### 0.10.3

- Add support for Scala 3.0.0

### 0.10.2

- Wrap text in help messages
- Add support for Scala 3.0.0-RC3

### 0.10.1

- Add support for Scala 3.0.0-RC2

### 0.10.0

- Add readers for `() => InputStream`, `() => OutputStream` and `geny.Readable`.
  These readers follow the convention of using '-' to read/write from
  stdin/stdout.
- Change the parser to support inserting parameters during parsing. Predefs can
  now be specified as parameters.

### 0.9.0

- Show default values of named parameters in help messages.
- Implement XDG Base Directory Specification.
- Introduce the concept of a "predef", a flat configuration file which contains
  command line arguments

### 0.8.0 and before

A command line parser for Scala 2 and 3, featuring:
- simple interfaces
- bash completion

## Glossary

**parameter**
: a named variable in an command line definition

**argument**
: the value assigned to a parameter

***named* argument**
: an argument that starts with `--`. The characters following determine the name
of the parameter that the argument is assigned to. The actual value assigned to
the parameter is given after an '=' or a spance. For instance `--foo=bar`
assigns `bar` to `foo`. Named arguments may appear in any order on a command
line.

***positional* argument**
: an argument that is not named. Positional arguments are assigned to positional
parameters according to their respective order of occurence.
