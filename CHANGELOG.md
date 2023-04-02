# Changelog

## 0.20.0

This release focuses on changes to the annotation-based API. The minimum Scala 3
version has been bumped to 3.2

- Refactor the annotation-based macro parser to work with Scala 3.2 and avoid
  some strange compiler crashes.
- Include the exception class's name in the default error message,

## 0.19.1

- Add support for defining top-level main functions with nested commands.

## 0.19.0

- Add error handling and output printing to the annotation API.

## 0.18.1

- Fix flag-derivation in the annotation API.

## 0.18.0

- Migrate documentation from custom static site to mdbook.
- Improve documentation
- Replace general `arg()` annotation with more specialized versions

## 0.17.0

### Breaking

- Rework annotation-based macro API and remove experimental status. This is now
  the recommended interface for basic use cases.

- Replace sub-commands with more structured sub-parsers.

  While the idea of independent subcommands seems quite elegant, there are
  unfortunately a couple of pitfalls which make them quite brittle to use. The
  first is that the current API made it easy to capture arguments at the wrong
  time in lambdas, hence leading to incomplete argument errors. The second, more
  fundamental issue, is that independent commands are by definition independent,
  and hence generating shared help messages and bash completion is quite tricky.
  For example, we needed to resort to hacky workarounds that relied on the stack
  and thread locals in combination with "magic" parameters to achieve composable
  bash completion that worked with nested commands.

  Subparsers are less powerful but also less brittle and should still be
  suitable for the majority of usecases. In situations where absolute control is
  necessary, the user can still define an all-absorbing parameter and handle
  subcommands manually.

### Minor

- Remove parameter style checker.

### Experimental

- Add experimental configuration parsing library.

## 0.16.2

- Add low-level escape hatches for manually adding parameter descriptors to the
  argument parser and bash completion scripts.
- Add a hook provide hook for handling unknown subcommands.

## 0.16.1

Add support for annotation-based, macro-generated argument parsers
(experimental).

## 0.16.0

The major change in this release is the migration to a mixin-based API. The
`ArgumentParser` trait as well as all readers have been moved to traits in
`argparse.core._`, and the new top-level object for users is `argparse.default`.
Thus, any references to `argparse.ArgumentParser` need to updated to
`argparse.default.ArgumentParser`.

Other breaking changes:

- Disable most experimental macro-based parser. This includes `settings` and
  `mutableSetting`. A new macro-based parser is in the works.
- Remove `show()` function from `Reader`.
- Add a `typeName` function to `Reader`.
- Rename `BashCompletion` to `InteractiveBashCompletion`.
- Make argument a top-level class, instead of `() => A`.
- Remove deprecated features.

Other changes:

- Implement terminal properties for Native (the code between Native and JVM is
  now shared).
- Upgrade to Scala Native 0.4.4

## 0.15.2

- Add an INI printer
- Derive publish version from Git

## 0.15.1

- Cleanup INI parser support and add more tests.

## 0.15.0

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

## 0.14.0

Rename project to scala-argparse.

## 0.13.0

- Remove ability to read args from a file (predef reader). This caused ordering
  issues in the parser and seemed like a recipe for obsuring config origins.
- Implement an ini-style config parser, and allow params to read from config.

## 0.12.1

- Add range reader.
- Upgrade mill to 0.9.10

## 0.12.0

- Add experimental case class parser (Scala 3 only), available under
  `parser.settings` (the previous mutable settings parser has been renamed to
  `parser.mutableSettings`).
- Add readers for:
  - `scala.concurrent.time.Duration`
  - Common `java.time` data types
  - Collections of paths. These readers use `:` as a separator, instead of the
    usual `,`'.
- Upgrade to Scala 3.0.2.

## 0.11.0

- Upgrade to Scala 2.13.6
- Refactor XDG directory implemention
- Refactor default help message

## 0.10.3

- Add support for Scala 3.0.0

## 0.10.2

- Wrap text in help messages
- Add support for Scala 3.0.0-RC3

## 0.10.1

- Add support for Scala 3.0.0-RC2

## 0.10.0

- Add readers for `() => InputStream`, `() => OutputStream` and `geny.Readable`.
  These readers follow the convention of using '-' to read/write from
  stdin/stdout.
- Change the parser to support inserting parameters during parsing. Predefs can
  now be specified as parameters.

## 0.9.0

- Show default values of named parameters in help messages.
- Implement XDG Base Directory Specification.
- Introduce the concept of a "predef", a flat configuration file which contains
  command line arguments

## 0.8.0 and before

A command line parser for Scala 2 and 3, featuring:
- simple interfaces
- bash completion
