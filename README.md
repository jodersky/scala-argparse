# cmdr

Pragmatic command line parsing and configuration for Scala apps.

## Guiding Principles

- Avoid ceremony and target the common use-case. *The design is inspired by
  the [`@main` annotation](https://dotty.epfl.ch/docs/reference/changed-features/main-functions.html)
  available in Scala 3 and the [argparse](https://docs.python.org/3/library/argparse.html)
  package from python.*

- Read configuration from the environment. *This encourages separation of config
  from code, as described in "the 12 factor app" https://12factor.net/config.*

## Example

```scala
object Main {

  @cmdr.main("serverapp", "0.1.0")
  def main(
      host: String = "localhost",
      port: Int = 8080,
      path: java.nio.file.Path
  ): Unit = {
    println(s"$host:$port$path")
  }

}
```

1. Build the above application by running `./mill examples.serverapp.dist`.

2. Try running the `./serverapp` executable:

```shell
$ ./serverapp
missing argument: path
try 'serverapp --help' for more information
```

```
$ ./serverapp --help
usage: serverapp [--port=<value>] [--host=<value>] <path>
```

```shell
# method params without defaults map to positional args
$ ./serverapp /srv/www
localhost:8080/srv/www
```

```shell
# method params with defaults map to named args
$ ./serverapp --port=9090 /srv/www
localhost:9090/srv/www
```

```shell
# you can also override named params with environment variables
$ export SERVERAPP_HOST="0.0.0.0"
$ ./serverapp --port=9090 /srv/www
0.0.0.0:9090/srv/www
```

```shell
# all parse errors are displayed; not just the first
$ ./serverapp --port="aaaahhhhh" a b c
unknown argument: b
unknown argument: c
error processing argument --port: 'aaaahhhhh' is not an integral number
try 'serverapp --help' for more information
```

## Usage

This library is published on maven central and may be obtained by adding the
following coordinates to your build:

- mill: `ivy"io.crashbox::cmdr:<version>"`
- sbt: `"io.crashbox" %% "cmdr" % "<version>"`

where `<version>` is given by [![Latest version](https://index.scala-lang.org/jodersky/cmdr/cmdr/latest.svg)](https://index.scala-lang.org/jodersky/cmdr/cmdr)

Note that this library requires Scala 2.13 and **requires the scalac option
"-Ymacro-annotations" to be enabled**. It may also be possible to use this
library with Scala 2.12 and the macro-paradise plugin.

## Details

Look at [the API docs](https://jodersky.github.io/cmdr/cmdr/package$$main.html) ([defined here](cmdr/src/cmdr/package.scala)) for parsing rules and
explanations on how it works.

## Glossary

The documentation frequently refers to 'named' and 'positional' parameters.
These are defined as follows:

**parameter**
: a named variable in an interface definition (command line or method)

**argument**
: the value assigned to a parameter

***named* argument**
: an argument that starts with `--`. The characters following determine the name
  of the parameter that the argument is assigned to. The actual value assigned to
  the parameter is given after an '='. For instance `--foo=bar` assigns `bar` to
  `foo`.
  Named arguments may appear in any order on a command line.

***positional* argument**
: an argument that is not named. Positional arguments are assigned to positional
  parameters according to their respective order of occurence.
