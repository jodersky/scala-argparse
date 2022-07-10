
class Foo

// object api extends argparse.core.Api with argparse.core.MainArgsApi

val entrypoints = argparse.default.initialize()

@argparse.main()
def foo(value: Int = 2, paths: List[os.Path] = Nil) = {
  println(value)
  println(paths)
}

@argparse.main()
def bar() = {

}

def main(args: Array[String]) = {
  //println(entrypoints.te)
  //println(entrypoints.methods)

  val parser = argparse.default.ArgumentParser()

  parser.param("--fooBar", "")
  parser.parseOrExit(args)

  //entrypoints.methods.head.run(args)
}
