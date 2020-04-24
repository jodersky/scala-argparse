object Main {

  @cmdr.main("test")
  def main(
      configDir: os.FilePath,       // first position
      extraArgs: Seq[String],       // second position and subsequent
      host: String = "localhost",   // --host= or env var TEST_HOST=
      port: Int = 8080,             // --port= or env var TEST_PORT=
      authKey: String = "1234"      // --auth-key= or env var TEST_AUTH_KEY=
  ): Unit = {
    println(host)
    println(port)
    println(configDir)
    println(extraArgs)
  }

}
