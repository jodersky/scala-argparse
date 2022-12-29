package argparse.core

/** User-friendly parameter information, used for generating help message */
case class ParamInfo(
    isNamed: Boolean,
    names: Seq[String],
    argName: Option[String], // if this is a named param, what should the argument be called in help messages
    repeats: Boolean,
    env: Option[String],
    description: String,
    interactiveCompleter: String => Seq[String],
    standaloneCompleter: argparse.BashCompleter
) {
  def isFlag = isNamed && argName == None
}
