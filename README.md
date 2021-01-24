# cmdr

Pragmatic command line parsing for Scala applications.

## Guiding Principles

- Avoid ceremony and target the common use case. *The design is inspired by the
  [argparse](https://docs.python.org/3/library/argparse.html) package from
  python.*

- Read configuration from the environment. *This encourages separation of config
  from code, as described in "the 12 factor app" https://12factor.net/config.*

## Example

```scala
def main(args: Array[String]): Unit = {
  val parser = cmdr.ArgumentParser()

  val host = parser.param[String](
    "--host",
    default = "localhost"
  )

  val port = parser.param[Int](
    "--port",
    default = 8080,
    aliases = Seq("-p"),
    env = "PORT"
  )

  val path = parser.requiredParam[java.nio.file.Path](
    "path"
  )

  parser.parse(args)
  println(s"${host()}:${port()}${path()}")
}
```

1. Build the above application by running `./mill examples.readme.dist`.

2. Run the `./readme` executable:

```shell
$ ./readme
missing argument: path
try ' --help' for more information
```

```shell
$ ./readme --help
Usage:  [OPTIONS] <path>



Options:
  --help               Show this message and exit
  --host=
  --port=, -p=
  --version            Show the version and exit

Environment:
  PORT                 --port
```

```shell
$ ./readme /srv/www
localhost:8080/srv/www
```

```shell
$ ./readme --port=9090 /srv/www
localhost:9090/srv/www
```

```shell
$ ./readme /srv/www --port=9090
localhost:9090/srv/www
```

```shell
$ PORT=80 ./readme /srv/www --host=0.0.0.0
0.0.0.0:80/srv/www
```

```shell
# all parse errors are displayed; not just the first
$ ./readme --port="aaaahhhhh" a b c
unknown argument: b
unknown argument: c
error processing argument --port: 'aaaahhhhh' is not an integral number
try '--help' for more information
```

## Usage

This library is published on maven central and may be obtained by adding the
following coordinates to your build:

- mill: `ivy"io.crashbox::cmdr:<version>"`
- sbt: `"io.crashbox" %% "cmdr" % "<version>"`

where `<version>` is given by [![Latest
version](https://index.scala-lang.org/jodersky/cmdr/cmdr/latest.svg)](https://index.scala-lang.org/jodersky/cmdr/cmdr)

This library is published for Scala 2.13 and Dotty.

- Under Scala 2.13, the additional macros **require the scalac option
  "-Ymacro-annotations" to be enabled**.

- It may also be possible to use this library with Scala 2.12 and the
  macro-paradise plugin.

## Documentation

Look at [the API docs](https://jodersky.github.io/cmdr/cmdr/package$$main.html)
([defined here](cmdr/src/cmdr/package.scala)) for parsing rules and explanations
on how it works.

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
