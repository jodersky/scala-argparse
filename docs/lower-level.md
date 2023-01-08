This is the documentation for the lower-level interface. It is used by the
annotation-based [higher-level interface](index.html), but offers more flexibility.

## Tutorial

*Acknowledgement: this tutorial is inspired by and largely copied from the
[Python Argparse Tutorial by Tshepang
Lekhonkhobe](https://docs.python.org/3/howto/argparse.html), made available
under the Zero Clause BSD License.*

This tutorial is intended to be an introduction to argparse.

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
: a named variable, a placeholder, in a command line definition

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
$include:examples/basic/src/main.scala$
```

The following is a result of running the code:

```
$include:examples/basic/src/shell.txt$
```

Here is what is happening:

- Running the program without any arguments results in nothing displayed to
  stdout. Not so useful.

- The second one starts to display the usefulness of the `argparse` library. We
  have done almost nothing, but already we get a help message.

- The `--help` option is the only option we get for free (i.e. no need to
  specify it). Specifying anything else results in an error. But even then, we
  do get a useful usage message, also for free.

### Introducing Required Parameters

An example:

```scala
$include:examples/paramreq/src/main.scala$
```

And running the code:

```
$include:examples/paramreq/src/shell.txt$
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
$include:examples/paramreq2/src/main.scala$
```

And we get:

```
$include:examples/paramreq2/src/shell.txt$
```

Now, how about doing something even more useful:

```scala
$include:examples/paramreq3/src/main.scala$

```

Following is a result of running the code:

```
$include:examples/paramreq3/src/shell.txt$
```

That went well. The program now even helpfully quits on bad input.

### Introducing Optional Parameters

So far the parameters that we have specified were required. Let's look at how we
can make an argument optional.

An example:

```scala
$include:examples/paramopt/src/main.scala$
```

Following is a result of running the code:

```
$include:examples/paramopt/src/shell.txt$
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
$include:examples/paramrep/src/main.scala$
```

Following is a result of running the code:

```
$include:examples/paramrep/src/shell.txt$
```

Here is what's happening:

- The parameter is made **repeated** by declaring it with the `repeatedParam()`.

- Repeated parameters accumulate all ocurences in the argument holder.

  Thus, here, `value` will give us back a `Seq[os.FilePath]` rather than a single
  `os.FilePath`.

### Named Parameters and Positional Parameters

So far you may have noticed that all our examples have used *positional*
parameters. Recall from the initial `ls` example, that a positional parameter is
one which is set solely based on the position of its argument. When parameter
lists get very long or change over time, it can become very difficult to keep
matching argument lists coherent. Therefore, most command line tools will mostly
use *named* parameters.

An example:

```scala
$include:examples/paramnamed/src/main.scala$
```

Following is a result of running the code:

```
$include:examples/paramnamed/src/shell.txt$
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
$include:examples/paramflag/src/main.scala$
```

Running it:

```
$include:examples/paramflag/src/shell.txt$
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
$include:examples/paramshort/src/main.scala$
```

And here goes:

```
$include:examples/paramshort/src/shell.txt$
```

Note that the new aliases are also reflected in the help text.

### Reading from the Environment

In some cases it can be useful to fall back to reading a command line from an
environment variable.

Example:

```scala
$include:examples/paramenv/src/main.scala$
```

Run it:

```
$include:examples/paramenv/src/shell.txt$
```

How it works:

- Required and optional parameter declarations can specify an `env`, which will
  name an environment variable to use if the argument cannot be found on the
  command line.

- The order of precedence is:
  1. the argument on the command line
  2. the environment variable
  3. the default value

---

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
$include:examples/subparsers/src/main.scala$
```

```
$include:examples/subparsers/src/shell.txt$
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
$include:examples/completion1/src/main.scala$
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
$include:examples/completion2/src/main.scala$
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
$include:examples/paramdep/src/main.scala$
```

```
$include:examples/paramdep/src/shell.txt$
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
$include:examples/reader/src/main.scala$
```

```scala
$include:examples/reader/src/shell.txt$
```

How it works:

- `Reader` is a *typeclass* which is responsible for parsing strings from the
  command line into instances of Scala types.

- Readers are declared in an API bundle. An API bundle is a bunch of traits that
  are mixed together in order to define "a flavor" of argparse.

  The default bundle implemented in this library is `argparse.default`, which
  includes Readers for most common types.

- You can create a custom bundle by creating an object which extends
  `argparse.core.Api`, and declare additional readers in it.

- The `ArgumentParser` from the custom bundle will find the *Reader* instance
  which can parse the desired parameter type.

### Writing Man Pages

The built-in help message system is useful for quick reference, but is too terse
for thoroughly documenting command line applications. For this, I recommend that
you write a **man page** and ship it alongside every application that you
create.

I recommend that you watch the presentation ["Man, ‘splained: 40 Plus Years of
Man Page History", by Breanne
Boland](https://www.youtube.com/watch?v=_UjJMrahc8o&list=UU3Pk-8hhzME2w5BL_JvXfRg&index=16).
It goes into the reasons and best-practices of writing man pages. You can also
read the manual page's manual page (run `man man`) if you like.

#### Template

Instead of writing a manual page by hand in
[troff](https://en.wikipedia.org/wiki/Troff), you can use the following markdown
template and run it through [pandoc](https://pandoc.org/).

````markdown
---
title: MY-APP
section: 1
header: User Manual
footer: App 1.0.0
date: June 2022
---

# NAME

my-app \- do something

# SYNOPSIS

**`my-app [--option1 <name>] [--option2 <name>] <arg>`**

# DESCRIPTION

An application which does something useful with `<arg>`.

The description can go into details of what the application does, and can span
multiple paragraphs.

## A subsection

You can use subsections in the description to go into finer details.

# OPTIONS

**`--option1=<string>`**
: An arbitrary string which sets some specific configuration value. Defaults to
some sane value.

**`--option2=<string>`**
: Another arbitrary string which sets some specific configuration value.
Defaults to some sane value.

# EXIT STATUS

Return 0 on success, 1 on error.

# ENVIRONMENT

`MY_APP_VARIABLE`
: Environment variable used by this application.

# FILES

`/etc/my-app.conf`
: This is an important file. Configuration values are read from this file if
they are not specified on the command-line

# EXAMPLES

**An example tells a thousand words.**

```
you can use markdown code sections
```

**Please include at least one example!**

# SEE ALSO

[A reference](https://pandoc.org/)
````

To preview the page after editing, run:

```
pandoc -s -f markdown-smart manpage.md -t man | man -l -
```

(the `-smart` is necessary here, to avoid converting '\-\-' into an em-dash)

And, once ready, save it as a man page:

```
pandoc -s -f markdown-smart manpage.md -t man > manpage.1
```

then finally ship it alongside your application.

Since it's written in markdown, you can also convert to html and make it
available online.

---
