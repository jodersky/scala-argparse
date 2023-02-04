## Usage

Annotate the desired main function with `command()`. Scala-argparse will then
generate a standard main method and command line parser with bells-and-whistles
such as help messages and bash completion.[^1]

[^1]: Note that until Scala 3 supports macro annotations (probably in version
    3.3.0), you will need to write a tiny boilerplate snippet as shown in the
    introductory example.

Note: the generated code uses a lower-level interface, which can also be
directly used for more flexibility. It is suggested that you [read the
tutorial](./ll/index.html) in any case, for further understanding of how
argument parsing works at a lower level.

### Parameter Mapping

Scala method parameters will be mapped to command line parameters in the
following way:

- Parameters with defaults will become `--named-parameters=` on the command
  line. Furthermore, boolean parameters become `--flags`, meaning that they
  don't need to take a 'true' argument on the command line.

- Parameters without defaults become positional parameters.

- Parameters of type `Seq[?]` become repeatable parameters on the command line.

E.g.

```scala
{{#include ../../examples/annotation-mappings/src/main.scala}}
```

```
{{#include ../../examples/annotation-mappings/src/shell.txt}}
```

### Parameter Types

Support for reading arguments from the command line as Scala types is provided
for many types out-of-the-box. Some examples:

- numeric types
- `java.io`, `java.nio` and `os.Path` file types
- various `java.time` date types
- `key=value` pairs of other supported types

The mechanism in which command line arguments are converted to Scala types is
highly customizable and [new types can easily be
added](./ll/cookbook.md#adding-support-for-a-new-type-of-parameter).

E.g.

```scala
{{#include ../../examples/annotation-types/src/main.scala}}
```

```
{{#include ../../examples/annotation-types/src/shell.txt}}
```

### Parameter Overrides

The generated command line parameters can further be customized by annotating
Scala parameters with certain annotations:

- [`@alias()`](javadoc/api/argparse/alias.html): set other names by which the
  parameter will be available. This is particularly useful for defining
  single-letter short names for frequently used parameters.

- [`@env()`](javadoc/api/argparse/env.html): set the name of an environment
  variable which will be used to lookup the parameter if it is not found on the
  command line.

- [`@name()`](javadoc/api/argparse/name.html): override the name derived from
  the parameter name. This can be used as an escape hatch for changing
  positional to named arguments and vice versa.

E.g.

```scala
{{#include ../../examples/annotation-annot/src/main.scala}}
```

```
{{#include ../../examples/annotation-annot/src/shell.txt}}
```

### Output Mapping

The returned values of annotated functions are automatically converted to
strings and printed to standard out. There are builtin conversions for some
common return values:

- iterables of products (aka case classes) are printed in a tabular format
- other iterables are printed one per line
- byte arrays and other sources of binary data are streamed
- futures are awaited

In other cases, the `toString` method of the returned value is simply called.

E.g.

```scala
{{#include ../../examples/annotation-output/src/main.scala}}
```

```
{{#include ../../examples/annotation-output/src/shell.txt}}
```

You can also define your own conversions by defining instances of the
`argparse.core.OutputApi#Printer` typeclass.

### Error Handling

In case a command throws, only the exception's message is printed. The stack trace
is not shown unless a `DEBUG` environment variable is defined.

You can change this behavior by overriding the `handleError` function of the
`OutputApi` trait.

### Nested Commands

As an application grows, it is common to organise different "actions" or
"flavours" of the application under nested commands, each taking their own list
of parameters. See the `git` or `docker` tools for some such examples.

In scala-argparse, nested commands use the same mechanism as single, top-level
commands, with one small twist: instead of annotating a *method* with
`command()`, you annotate a *class definition* (or a function that returns an
instance of an annotated class). This can be done recursively, and classes can
declare parameters which can be referenced by child commands.

E.g.

```scala
{{#include ../../examples/annotation-commands/src/main.scala}}
```

```
{{#include ../../examples/annotation-commands/src/shell.txt}}
```

### Bells and Whistles

Any program that uses scala-argparse automatically gets:

- A concise help dialogue (that is formatted according to your terminal's
  current dimensions) derived from the main function's scaladoc comment.

  You can view the help dialogue by passing the `--help` flag.

- A bash-completion script, which will allow users to get tab-completion in
  their terminal.

  The bash completion script can be generated by passing a
  `--bash-completion=<program name>` argument.

- Bash-awareness for interactive bash completion.

## Next Steps

- Now that you know the high-level API, check out the [lower-level
  API](./ll/index.html), which underpins the former and can be helpful for
  understanding cusomizations.

- Read the API docs. Start with the [`argparse.default`
  bundle](javadoc/api/argparse/default$.html).
