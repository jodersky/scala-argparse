# cmdline

Pragmatic argument parsing and configuration for Scala apps.

# Guiding Principles

- Embrace system standards and conventions. *It is assumed that the application
  is run as a first-class executable. All config is read from command line
  arguments and environment variables provided by the OS. We don't rely on
  JVM-only features such as system properties or config files in resource
  loaders.*

- Read configuration from the environment. *This encourages separation of config
  from code, as described in "the 12 factor app" https://12factor.net/config.*

- Avoid ceremony and target the common use-case. *The design is inspired by
  the [`@main` annotation](https://dotty.epfl.ch/docs/reference/changed-features/main-functions.html)
  available in Scala 3 and the [argparse](https://docs.python.org/3/library/argparse.html)
  package from python.*

# Example

```scala
object Main {

  @cmdr.main("serverapp", "0.1.0")
  def main(
      dbConnection: String = "jdbc:sqlite:local.db"
      host: String = "localhost",
      port: Int = 8080,
      webRoot: java.nio.Path
  ): Unit = {
    println("Actual config:")
    println(dbConnection)
    println(host)
    println(port)
    println(webRoot)
  }

}
```

Assuming the above application has been compiled and assembled into the
executable 'serverapp' (take a look at [mill
assembly](http://www.lihaoyi.com/mill/index.html) or
[sbt-assembly](https://github.com/sbt/sbt-assembly) on how that can be done):

```shell
$ ./serverapp --port=9090 /srv/www
Actual config:
jdbc:sqlite:local.db
localhost
9090
/srv/www
```

```shell
# you can also override named params with environment variables
$ export SERVERAPP_DB_CONNECTION="jdbc:postgresql:proddb"
$ ./serverapp --port=9090 /srv/www
Actual config:
jdbc:sqlite:proddb
localhost
9090
/srv/www
```

```
$ ./serverapp
missing argument: web-root
try passing `--help` for more information
(exit 2)
```

# Getting Started

- maven repo
- requires Scala 2.13

# Details

Look at [this API doc]() for parsing rules and explanations on how it works.

# Glossary

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
