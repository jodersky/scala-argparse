object Main {

  @cmdr.main("test", "This shows how aliases and help messages can be attached to parameters.")
  def main(
      @cmdr.alias("-a")
      @cmdr.help("The authentication key")
      authKey: String = "1234" // --auth-key= or env var TEST_AUTH_KEY=
  ): Unit = {
    println(authKey)
  }

}
