package cmdr

case class main(
  name: String = "",
  doc: String = "",
  version: String = ""
) extends annotation.StaticAnnotation

case class arg(
  aliases: Seq[String] = Seq(),
  doc: String = "",
  env: String = null
) extends annotation.StaticAnnotation
