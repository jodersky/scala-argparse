package argparse

case class main() extends annotation.StaticAnnotation

case class arg(
    aliases: Seq[String] = Seq(),
    doc: String = "",
    env: String = null
) extends annotation.StaticAnnotation
