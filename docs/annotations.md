# WIP

## Example

```scala
@arpgarse.main()
def main(host: String = "localhost", port: Int = 8080, secure: Boolean = false, path: os.Path) = {
  val scheme = if (secure) "https" else "http"
  val url = s"$scheme://${host}:${port}/${path}"
  println(url)
}

// this is necessary for now, but will become obsolete once scala 3 supports annotation macros
def main(args: Array[String]) = argparse.default.main(this, args)
```

Run it:

```
app
app --host=1.1.1.1 --secure --port 9090 /a/b
app --help
```

### Basic

```scala
@arpgarse.main()
def main() =
  ()

// this is necessary for now, but will become obsolete once scala 3 supports annotation macros
def main(args: Array[String]) = argparse.default.main(this, args)
```

```
app
app a
app --verbose
```

### Positional Parameters

```scala
@arpgarse.main()
def main(firstParam: String, secondParam: Int) =
  println(firstParam)
  println(secondParam)

// this is necessary for now, but will become obsolete once scala 3 supports annotation macros
def main(args: Array[String]) = argparse.default.main(this, args)
```

```
app
app hello
app hello 42
app hello world
app --help
```

### Named Parameters

```scala
@arpgarse.main()
def main(firstParam: String = "first", secondParam: Int = 42) =
  println(firstParam)
  println(secondParam)

// this is necessary for now, but will become obsolete once scala 3 supports annotation macros
def main(args: Array[String]) = argparse.default.main(this, args)
```

```
app
app --first-param=hello  --second-param=42
app --help
```

### Flags

```scala
@arpgarse.main()
def main(verbose: Boolean = false) =
  println(verbose)

// this is necessary for now, but will become obsolete once scala 3 supports annotation macros
def main(args: Array[String]) = argparse.default.main(this, args)
```

```
app
app --verbose
```

### Repeated Arguments

```scala
@arpgarse.main()
def main(files: Seq[os.FilePath]) =
  println(files)

// this is necessary for now, but will become obsolete once scala 3 supports annotation macros
def main(args: Array[String]) = argparse.default.main(this, args)
```

```
app
app file1
app file1 file2 file3
```

### Context Capture

```scala
class Context(base: os.Path):
  @arpgarse.main()
  def main(file: os.FilePath = base / "default") =
    println(file)

def main(args: Array[String]) = argparse.default.main(Context(os.root), args)
```

```scala
class Context(base: os.Path):
  @arpgarse.main()
  def main(file: os.FilePath = base / "default") =
    println(file)

def main(args: Array[String]) =
  val parser = argparse.default.ArgumentParser()
  val base = parser.param[os.Path](
    "--base",
    os.root,
    endOfNamed = true
  )
  val subargs = parser.repeatedParam[String]("subargs")
  parser.parseOrExit(args)
  argparse.default.main(Context(base.value), subargs.value)
```

### Commands

```scala
class Context(base: os.Path):
  @arpgarse.main()
  def foo(file: os.FilePath = base / "default") =
    println(file)

  def bar(file: os.FilePath = base / "default") =
    println(file)

def main(args: Array[String]) =
  val parser = argparse.default.ArgumentParser()
  val base = parser.param[os.Path](
    "--base",
    os.root
  )
  parser.command("foo"){

  }
  parser.command("bar"){

  }
```

```scala
def main(args: Array[String]) =
  val parser = argparse.default.ArgumentParser()
  val base = parser.param[os.Path](
    "--base",
    os.root
  )
  for entrypoint <- argparse.default.findMains[Context] do
    parser.command(entrypoint.name, entrypoint.invoke(Context(base.value)))
```

```scala
def main(args: Array[String]) =
  val parser = argparse.default.ArgumentParser()
  val base = parser.param[os.Path](
    "--base",
    os.root
  )
  parser.commands(Context(base.value))
```
