package object cmdr {

  /** Get the system arguments eagerly, this allows using them in a constructor,
    * outside of main().
    *
    * This may be somewhat of a hack.
    */
  val argsv = System.getProperty("sun.java.command").split(" ").tail

  @deprecated("use ArgParser() instead", "0.3.0")
  type ArgumentParser = ArgParser
  val ArgumentParser = ArgParser
}
