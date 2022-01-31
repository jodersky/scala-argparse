package object argparse {

  /** An argument accessor is a function that returns an argument, assuming that
    * parsing was successful.
    */
  type Arg[A] = () => A

  /** Get the system arguments eagerly, this allows using them in a constructor,
    * outside of main().
    *
    * This may be somewhat of a hack.
    */
  val argsv = System.getProperty("sun.java.command").split(" ").tail

  @deprecated("use ArgumentParser() instead", "0.14.0")
  type ArgParser = ArgumentParser
  @deprecated("use ArgumentParser() instead", "0.14.0")
  val ArgParser = ArgumentParser

  @deprecated("use userdirs instead", "0.10.3")
  type xdg = userdirs.type
  @deprecated("use userdirs instead", "0.10.3")
  val xdg = userdirs

}
