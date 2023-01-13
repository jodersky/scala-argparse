# scala-argparse

[![project docs](https://img.shields.io/badge/docs-website-blueviolet)](https://jodersky.github.io/scala-argparse)
[![discord](https://img.shields.io/badge/chat-discord-blue)](https://discord.gg/usj9DC8FDN)
[![project chat](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://crashbox.zulipchat.com/#narrow/stream/330744-argparse)
[![latest version][scaladex-badge]][scaladex-link]
[![stability: firm](https://img.shields.io/badge/stability-firm-silver)](https://www.crashbox.io/stability.html)

[scaladex-badge]: https://index.scala-lang.org/jodersky/scala-argparse/argparse/latest.svg
[scaladex-link]: https://index.scala-lang.org/jodersky/scala-argparse/argparse

Pragmatic command line parsing for Scala applications.

## Highlights

- Simple interface, inspired by the
  [argparse](https://docs.python.org/3/library/argparse.html) package from
  python.

- Bash completion.

  - Standalone bash completion for a super snappy user experience, even on the
    JVM.

  - Interactive bash completion for the most custom needs.

- Works with Scala 2 and 3, Native and JVM

## Docs

- HTML: look at the [website](https://jodersky.github.io/scala-argparse)
- source (markdown): browse the docs/ folder.

## Building

This project uses [Mill](https://github.com/com-lihaoyi/mill) to build. The
configuration is in the `build.sc` file.

### Developer

- compile main project for all supported versions of scala: `./mill argparse.__.compile`
- run all tests: `./mill __.test`
- run an example:
  - `./mill examples.<name of example> <arguments>`
  - e.g. `./mill examples.paramnamed --verbosity 5`
  - note: examples use Scala Native and hence require llvm
- publish main project locally: `./mill argparse.__.publishLocal`

### Maintainer

Look at the scripts in the `ci/` directory.
