package object cmdr {

  /** Get the system arguments eagerly, this allows using them in a constructor,
    * outside of main().
    *
    * This may be somewhat of a hack.
    */
  val argsv = System.getProperty("sun.java.command").split(" ").tail

  type ArgumentParser = parsing.ArgumentParser
  val ArgumentParser = parsing.ArgumentParser

  /** Annotate this to a method to be used as the program's entrypoint.
    *
    * This generates a synthetic main method, converting command line arguments
    * to values which are then be passed to the function.
    *
    * = Example =
    *
    * {{{
    * object Main {
    *
    *   @cmdr.main("appname", "version")
    *   def entrypoint(
    *     host: String = "localhost",
    *     port: Int = 8080,
    *     authToken: String = "",
    *     to: java.nio.file.Path,
    *     files: Seq[java.nio.file.Path]
    *   ) = ???
    *
    * }
    * }}}
    *
    * Will lead to an executable program with the following usage line:
    *
    * {{{
    * appname [--host=] [--port=] [--auth-token=] to [files...]
    * }}}
    *
    * In other words, it allows overriding `host`, `port` and `authToken` with
    * named arguments (using the default value if absent), requires the user to
    * specify `to`, and allows multiple repeating arguments for `files`.
    *
    * Environment variable overrides are also available: `APPNAME_HOST`,
    * `APPNAME_PORT` and `APPNAME_AUTH_TOKEN` will be used if they are defined
    * and no corresponding argument is passed.
    *
    * = Mapping Rules =
    *
    * Function parameters are mapped to command-line parameters according to the
    * following rules (in order of precedence):
    *
    * 1. A function parameter *with a default value* is associated to a named,
    *    optional command line argument and environment variable.
    *
    *    - The command line name is given as the `--kebab-case` transformation
    *      of the function parameter name.
    *    - The environment variable name is given as the `UPPER_SNAKE_CASE`
    *      transformation, with the program's name prepended.
    *
    * 2. A parameter *without a default value* is associated a positional,
    *    required parameter.
    *
    * 3. A parameter without a default value *and of type Seq[_]* is associated
    *    all extranous positional parameters.
    *
    * = How It Works =
    *
    * A synthetic main function is appended after the annottee. This main function
    * defines a command line parser according to the above listed rules, and then
    * calls the annottee.
    *
    * E.g. the above example will lead to a synthetic main similar to the following:
    * {{{
    * def main(args: Array[String]): Unit = {
    *   val parser = _root_.cmdr.ArgumentParser("appname", "version");
    *   val p$macro$1 = parser.param[String]("--host", "localhost", "APPNAME_HOST"));
    *   val p$macro$2 = parser.param[Int]("--port", 8080, "APPNAME_PORT"));
    *   val p$macro$3 = parser.param[String]("--auth-token", "", "APPNAME_AUTH_TOKEN"));
    *   val p$macro$4 = parser.requiredParam[java.nio.file.Path]("to");
    *   val p$macro$5 = parser.repeatedParam[java.nio.file.Path]("files");
    *   parser.parse(args);
    *   entrypoint(p$macro$1.get, p$macro$2.get, p$macro$3.get, p$macro$4.get, p$macro$5.get)
    * }
    * }}}
    *
    * @see [[cmdr.parsing.ArgumentParser]]
    */
  @annotation.compileTimeOnly(
    "this program must be compiled with -Ymacro-annotations enabled"
  )
  class main(name: String = "", version: String = "")
      extends annotation.StaticAnnotation {
    import scala.language.experimental.macros
    def macroTransform(annottees: Any*): Any = macro cmdr.macros.MainImpl.impl
  }

}
