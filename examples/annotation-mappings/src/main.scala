import argparse.default as ap

@ap.command()
def main(
  namedParameter: String = "a",
  flag: Boolean = false,
  repeatable: Seq[String] = Seq(),
  positional1: Int,
  positional2: Int,
  remaining: Seq[String]
) =
  println(s"namedParameter=$namedParameter")
  println(s"flag=$flag")
  println(s"repeatable=$repeatable")
  println(s"positional1=$positional1")
  println(s"positional2=$positional2")
  println(s"remaining=$remaining")

// boilerplate until Scala 3 supports macro annotations
def main(args: Array[String]) = argparse.main(this, args)
