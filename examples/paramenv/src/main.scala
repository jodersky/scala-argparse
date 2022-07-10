def main(args: Array[String]): Unit =
  val parser = argparse.default.ArgumentParser()
  val creds = parser.param[os.Path](
    "--credentials-file",
    default = os.root / "etc" / "creds",
    env = "APP_CREDENTIALS_FILE",
    help = "the file containing service credentials"
  )
  parser.parseOrExit(args)
  println(creds.value)
