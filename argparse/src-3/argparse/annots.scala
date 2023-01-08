package argparse

/** Annotate a method parameter with this annotation to override some aspects of
  * macro-generated code.
  *
  * In general, `null` means to let the macro generate code, and is usually the
  * default.
  *
  * @param name Override the name of the parameter. Note that the name will be
  * used as-is. In particular, this means that you need to specify leading
  * dashes for named parameters.
  *
  * @param aliases Set aliases for the parameter.
  *
  * @param aliases Set the environment variable from which this parameter may be
  * read if not specified on the command line.
  */
case class arg(
    name: String = null,
    aliases: Seq[String] = Seq(),
    env: String = null,
    interactiveCompleter: String => Seq[String] = null,
    standaloneCompleter: BashCompleter = null
) extends annotation.StaticAnnotation
