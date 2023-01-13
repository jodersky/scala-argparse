import argparse.default as ap

@ap.command()
def main(
  @ap.alias("-s", "--address") server: String = "a",
  @ap.env("APPLICATION_CREDENTIALS") creds: os.Path = os.pwd / "creds",
  @ap.name("--named") positional: Int
) =
  println(s"server=$server")
  println(s"creds=$creds")
  println(s"positional=$positional")

// boilerplate until Scala 3 supports macro annotations
def main(args: Array[String]) = argparse.main(this, args)
