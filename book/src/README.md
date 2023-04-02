Command line parsing for Scala applications.

## Example

In Scala:

```scala
{{#include ../../examples/annotation-intro/src/main.scala}}
```

In a terminal:

```
{{#include ../../examples/annotation-intro/src/shell.txt}}
```

## Highlights

- Simple interfaces

  - Top-level, annotation-based.

  - Lower-level interface inspired by the
    [argparse](https://docs.python.org/3/library/argparse.html) package from Python.

- Bash completion

  - Standalone bash completion for a super snappy user experience, even on the
    JVM.

  - Interactive bash completion for the most custom needs.

- Works with Scala 3.2, on the JVM and Native (the lower-level interface also
  works with Scala 2.13)

- Support for subcommands (aka "verbs")

## Binaries

This library is published for Scala 3.2 and 2.13, for the JVM and Native. It is
available on maven central under the coordinates:

- mill: `ivy"io.crashbox::argparse::{{#include ../../out/releaseVersion.dest/version}}"`

- sbt: `"io.crashbox" %%% "argparse" % "{{#include ../../out/releaseVersion.dest/version}}"`


## Getting Help

| Channel | Links |
|---------|-------|
| Chat | [![discord](https://img.shields.io/badge/chat-discord-blue)](https://discord.gg/usj9DC8FDN) [![project chat](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://crashbox.zulipchat.com/#narrow/stream/330744-argparse) |
| Issues | [GitHub](https://github.com/jodersky/scala-argparse/issues) |
