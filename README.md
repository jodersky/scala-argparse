# cmdr

Pragmatic command line parsing for Scala applications.

## Highlights

- Simple interfaces:

  - imperative-style, inspired by the [argparse](https://docs.python.org/3/library/argparse.html)
    package from python

  - declarative-style, inspired by Scala 3's `@main` annotation

- Embedded bash completion. Simply add `complete -o nospace -C <program>
  <program>` to `~/.bashrc`

## Example

Both examples hae the same command-line interface.

### 1. Imperative Style

```scala
object Main {
  def main(args: Array[String]): Unit = {
    val parser = cmdr.ArgParser(
      "readme",
      "An example application"
    )

    val host = parser.param[String](
      "--host",
      default = "localhost",
      help = "network host"
    )

    val port = parser.param[Int](
      "--port",
      default = 8080,
      aliases = Seq("-p"),
      env = "PORT",
      help = "some port"
    )

    val path = parser.requiredParam[java.nio.file.Path](
      "path",
      help = "the path to use"
    )

    parser.parseOrExit(args)
    println(s"${host()}:${port()}${path()}")
  }
}
```

### 2. Declarative Style

```scala
object Main {
  @cmdr.main(name = "readme", doc = "An example application")
  def main(
    @cmdr.arg(doc = "network host")
    host: String = "localhost",
    @cmdr.arg(doc = "some port", aliases = Seq("-p"), env = "PORT")
    port: Int = 8080,
    @cmdr.arg(doc="the path to use")
    path: java.nio.file.Path
  ) = {
    println(s"$host:$port$path")
  }

  def main(args: Array[String]): Unit = cmdr.parseOrExit(args)
}
```

1. Build the above application by running either:
  - `./mill examples.readme-imperative[3.0.0-M3].dist`
  - or, `./mill examples.readme-declarative[3.0.0-M3].dist`

2. Run the `./readme-<imperative|declarative>` executable:

```
$ ./readme
missing argument: path
run with '--help' for more information
```

```
$ ./readme --help
usage: readme [options] <path>

An example application

positional arguments:
 path           the path to use
named arguments:
 --help         show this message and exit
 --host=        network host
 --port=, -p=   some port
environment variables:
 PORT           --port
```

```shell
$ ./readme /srv/www
localhost:8080/srv/www
```

```
$ ./readme --port=9090 /srv/www
localhost:9090/srv/www
```

```
$ ./readme /srv/www --port=9090
localhost:9090/srv/www
```

```
$ PORT=80 ./readme /srv/www --host=0.0.0.0
0.0.0.0:80/srv/www
```

```
# all parse errors are displayed; not just the first
$ ./readme --port="aaaahhhhh" a b c
unknown argument: b
unknown argument: c
error processing argument --port: 'aaaahhhhh' is not an integral number
run with '--help' for more information
```

## Usage

This library is published for Scala 2.13 (JVM and Native), and 3. It is
available on maven central under the coordinates:

- mill: `ivy"io.crashbox::cmdr::<version>"`
- sbt: `"io.crashbox" %%% "cmdr" % "<version>"`

where `<version>` is given by [![Latest
version](https://index.scala-lang.org/jodersky/cmdr/cmdr/latest.svg)](https://index.scala-lang.org/jodersky/cmdr/cmdr)

## Documentation

Look at [the API docs](https://jodersky.github.io/cmdr/cmdr/ArgParser.html)
([defined here](cmdr/src/cmdr/ArgParser.scala)) for parsing rules and
explanations on how it works.

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
