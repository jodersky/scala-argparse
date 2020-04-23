# cmdr

Pragmatic command line parsing and configuration for Scala apps.

## Guiding Principles

- Avoid ceremony and target the common use-case. *The design is inspired by
  the [`@main` annotation](https://dotty.epfl.ch/docs/reference/changed-features/main-functions.html)
  available in Scala 3 and the [argparse](https://docs.python.org/3/library/argparse.html)
  package from python.*

- Read configuration from the environment. *This encourages separation of config
  from code, as described in "the 12 factor app" https://12factor.net/config.*

- Embrace system standards and conventions. *It is assumed that the application
  is run as a first-class executable. All config is read from command line
  arguments and environment variables provided by the OS. We don't rely on
  JVM-only features such as system properties or config files in resource
  loaders.*

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

2. Play around with the `./serverapp` executable:

```shell
$ ./serverapp
missing argument: path
try passing `--help` for more information
```

```shell
$ ./serverapp /srv/www
localhost:8080/srv/www
```

```shell
$ ./serverapp --port=9090 /srv/www
localhost:9090/srv/www
```

```shell
# you can also override named params with environment variables
$ export SERVERAPP_HOST="0.0.0.0"
$ ./serverapp --port=9090 /srv/www
0.0.0.0:9090/srv/www
```

## Details

Look at [the API doc](cmdr/src/cmdr/package.scala) for parsing rules and
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
