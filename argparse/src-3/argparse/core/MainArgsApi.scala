package argparse.core

trait MainArgsApi extends TypesApi with LowPrioParamBuilders with ParsersApi:
  self =>

  def commandName(scalaName: String): String = TextUtils.kebabify(scalaName)
  def paramName(scalaName: String): String = TextUtils.kebabify(scalaName)

  /** Generate a main method for a single entry point.
    *
    * This method will become obsolete once annotation macros become available
    * in scala 3.
    */
  inline def dispatch[A](container: => A, args: Array[String], env: Map[String, String] = sys.env): Unit = ${
    Entrypoint.mainImpl[this.type, A]('self, 'container, 'args, 'env)
  }

  /** Mark a method as an application entrypoint.
    *
    * This will generate a main method, which will call the annotated method
    * after parsing the command line.
    *
    * The mapping between the CLI arguments to the annotated method is as follows:
    *
    * | Parameter Type | Has Default Value? | CLI
    * +----------------+--------------------+-----
    * | Boolean        | yes                | --named flag
    * | Boolean        | no                 | positional
    * | Iterable[A]    | yes                | --named repeated parameter
    * | Iterable[A]    | no                 | positional repeated paramater
    * | A              | yes                | --named parameter
    * | A              | no                 | positional paramter
    *
    * These mappings can be overridden by defining custom [[ParamBuilder]]s.
    */
  case class main() extends annotation.StaticAnnotation

  // convenience forwarders
  type arg = argparse.arg
  val arg = argparse.arg

  trait ParamBuilder[A]:
    def makeParams(
      name: String,
      description: String,
      default: Option[() => A],
      annot: argparse.arg,
      argparser: ArgumentParser
    ): () => A

  given (using reader: Reader[Boolean]): ParamBuilder[Boolean] with
    def makeParams(
      name: String,
      description: String,
      default: Option[() => Boolean],
      annot: argparse.arg,
      parser: ArgumentParser
    ) =
      parser.singleParam[Boolean](
        name = if default.isDefined then s"--$name" else name,
        default = default,
        env = Option(annot.env),
        aliases = annot.aliases,
        help = description,
        flag = true,
        endOfNamed = false,
        interactiveCompleter = None,
        standaloneCompleter = None,
        argName = None
      )

  given [A, Col[_] <: Iterable[_]](using reader: Reader[A]): ParamBuilder[Col[A]] with
    def makeParams(
      name: String,
      description: String,
      default: Option[() => Col[A]],
      annot: argparse.arg,
      parser: ArgumentParser
    ) =
      parser.repeatedParam(
        name = if default.isDefined then s"--$name" else name,
        aliases = annot.aliases,
        help = description,
      ).asInstanceOf[argparse.Argument[Col[A]]]

trait LowPrioParamBuilders:
  self: MainArgsApi =>

  given [A](using reader: Reader[A]): ParamBuilder[A] with
    def makeParams(
      name: String,
      description: String,
      default: Option[() => A],
      annot: argparse.arg,
      parser: ArgumentParser
    ) =
      parser.singleParam[A](
        name = if default.isDefined then s"--$name" else name,
        default = default,
        env = Option(annot.env),
        aliases = annot.aliases,
        help = description,
        flag = false,
        endOfNamed = false,
        interactiveCompleter = None,
        standaloneCompleter = None,
        argName = None
      )
