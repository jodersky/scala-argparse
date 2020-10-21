package cmdr

/** Annotate this to a method to be used as the program's entrypoint.
  *
  * A synthetic main method will be generated which will take care of parsing
  * command line args.
  *
  * = Example =
  *
  * {{{
  * object Main {
  *
  *   @cmdr.main("appname", "An example app.", "version")
  *   def entrypoint(
  *     @cmdr.help("Interface to listen on") host: String = "localhost",
  *     @cmdr.alias("-p") port: Int = 8080,
  *     authToken: String = "",
  *     to: java.nio.file.Path,
  *     files: Seq[java.nio.file.Path]
  *   ) = ???
  *
  * }
  * }}}
  *
  * The above will lead to an executable with the following help message:
  *
  * {{{
  * Usage: appname [OPTIONS] <to> <files>...
  *
  * An example app.
  *
  * Options:
  *   --auth-token=
  *   --help               Show this message and exit
  *   --host=              Interface to listen on
  *   --port=, -p=
  *   --version            Show the version and exit
  *
  * Environment:
  *   APPNAME_HOST         --host
  *   APPNAME_PORT         --port
  *   APPNAME_AUTH_TOKEN   --auth-token
  * }}}
  *
  * In other words, it allows overriding `host`, `port` and `authToken` with
  * named arguments (using the default value if absent), requires the user to
  * specify `to`, and allows multiple repeating arguments for `files`.
  *
  * Named arguments may also be set with environment variables. In the above,
  * `APPNAME_HOST`, `APPNAME_PORT` and `APPNAME_AUTH_TOKEN` will be used as
  * a fallback.
  *
  * = Parameter Mapping Rules =
  *
  * Function parameters are mapped to command-line parameters according to the
  * following rules (in order of precedence):
  *
  * 1. A function parameter *with a default value* is given a named, optional
  *    command line parameter and environment variable.
  *
  *      - The command line name is the `--kebab-case` transformation
  *        of the function parameter name.
  *
  *      - The environment variable name is the `UPPER_SNAKE_CASE`
  *        transformation, with the program's name prepended.
  *
  *    If the parameter is of type `Boolean`, the given command line parameter
  *    is a "flag" (see next section about parsing rules).
  *
  * 2. A parameter *without a default value* is given a positional,
  *    required parameter.
  *
  * 3. A parameter *without a default value and of type `Seq[_]`* is given
  *    all extranous positional parameters. It only makes sense to have one
  *    such parameter, as the first one will consume all remaining arguments.
  *
  * Command line arguments, which are just strings initially, are converted to
  * Scala values as defined by the [[cmdr.Reader]] typeclass. Here are
  * some common ones:
  *
  * | Type | Format Description | Example |
  * | ---- | ------------------ | ------- |
  * | `String` | Reads the argument as-is. |`--foo=hello`, `--foo="hello, world"` |
  * | `Int`, `Long` | Integral number. | `--foo=42` |
  * | `Float`, `Double` | Any number. | `--foo=42`, `--foo=1.5` |
  * | `Seq[_]`, `List[_]` | Comma-separated list. | `--foo=a1,a2,a3` |
  * | `java.nio.file.Path` | Any file path. | `--foo=/srv/www` |
  *
  * You may implement your own readers for other data types.
  *
  * Extra information, such as help string or aliases may added to
  * parameters via the [[cmdr.help]] and [[cmdr.alias]] annotations.
  *
  * = Command Line Syntax (a.k.a Parsing Rules) =
  *
  * The parser basically works by traversing all supplied command line
  * arguments linearly and acting upon them. It distinguishes between named
  * and positional arguments (and flags, but they're a special case of named).
  *
  * 1. Named parameters always start with "--". With the exception of flags,
  *    they always take an argument. This argument may either be given in an
  *    "embedded" way after an '=' sign, or as the next command line argument.
  *    In case it is given as the next command line argument, it must not
  *    start with a '--'.
  *
  *    A few examples:
  *
  *      - `--foo=hello` and `--foo hello` both set the parameter '--foo' to
  *        the value 'hello'.
  *
  *      - `--foo --bar=a` is an error, as no argument is supplied to `--foo`.
  *
  *      - `--foo=--bar` is accepted, as `--bar` is given as an "embedded"
  *        argument
  *
  *    Named arguments may be given in any order, even after positional
  *    arguments. Named arguments may be repeated, in which case the last
  *    argument trumps all previous ones.
  *
  * 2. Flags are named parameters that have been defined to not require an
  *    argument on the command line. In case a flag is encountered, its value
  *    is always set to the string "true", unless that values is embedded.
  *
  *    Some examples (assuming `--foo` is a flag and `--bar` isn't):
  *
  *    - `--foo` sets `--foo` to "true"
  *
  *    - `--foo a` sets `--foo` to "true" (`a` is considered a positional)
  *
  *    - `--foo=a` sets `--foo` to "a"
  *
  * 3. Positional arguments are all arguments that do not start with a `--`.
  *    They are read in order of occurrence and are assigned to the
  *    corresponding parameter.
  *
  * See [[cmdr.ArgumentParser]] for further details.
  *
  * = How It Works =
  *
  * A synthetic main function is appended after the annottee. This main
  * function defines a command line parser according to the above rules, and
  * then calls the annottee.
  *
  * E.g. the above example will lead to a synthetic main similar to the
  * following:
  *
  * {{{
  * def main(args: Array[String]): Unit = {
  *   val parser = _root_.cmdr.ArgumentParser("appname", "version");
  *   val p$macro$1 = parser.param[String]("--host", "localhost", "APPNAME_HOST"));
  *   val p$macro$2 = parser.param[Int]("--port", 8080, "APPNAME_PORT"));
  *   val p$macro$3 = parser.param[String]("--auth-token", "", "APPNAME_AUTH_TOKEN"));
  *   val p$macro$4 = parser.requiredParam[java.nio.file.Path]("to");
  *   val p$macro$5 = parser.repeatedParam[java.nio.file.Path]("files");
  *   parser.parse(args);
  *   entrypoint(p$macro$1(), p$macro$2(), p$macro$3(), p$macro$4(), p$macro$5())
  * }
  * }}}
  *
  * @see [[cmdr.ArgumentParser]]
  */
@annotation.compileTimeOnly(
  "this program must be compiled with -Ymacro-annotations enabled"
)
class main(name: String = "", description: String, version: String = "")
    extends annotation.StaticAnnotation {
  import scala.language.experimental.macros
  def macroTransform(annottees: Any*): Any = macro cmdr.macros.MainImpl.impl
}

/** Specify a command-line alias by which a parameter may be set. */
class alias(name: String) extends annotation.Annotation

/** Attach a help message to a parameter. */
class help(message: String) extends annotation.Annotation
