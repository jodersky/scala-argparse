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
