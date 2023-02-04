import argparse.default as ap

/** This is an example app. It shows how a command line interface can be
  * generated from various kinds of method parameters.
  *
  * @param server a sample named parameter
  * @param secure this is a flag
  * @param path a positional parameter
  */
@ap.command()
def main(
  server: String = "localhost",
  secure: Boolean = false,
  path: os.SubPath
): String =
  val scheme = if secure then "https" else "http"
  s"$scheme://$server/$path"

// boilerplate necessary until macro annotations become available in Scala 3
def main(args: Array[String]): Unit = argparse.main(this, args)
