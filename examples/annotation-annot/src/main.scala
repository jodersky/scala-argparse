import argparse.default as ap

@ap.command()
def main(
  @ap.arg(aliases = Seq("-s", "--address")) server: String = "a"
) =
  println(server)

// boilerplate until Scala 3 supports macro annotations
def main(args: Array[String]) = argparse.main(this, args)
