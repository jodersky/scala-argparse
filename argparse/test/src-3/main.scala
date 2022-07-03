
class Foo

// object api extends argparse.core.Api with argparse.core.MainArgsApi

val entrypoints = argparse.default.initialize()

@argparse.main()
def foo(value: Int = 2, paths: List[os.Path] = Nil, data: (String, os.Path) = ("", os.Path("."))) = {
  println(value)
  println(paths)
}

@argparse.main()
def bar() = {

}

def main(args: Array[String]) = {
  //println(entrypoints.te)
  //println(entrypoints.methods)

  entrypoints.methods.head.run(args)
}
