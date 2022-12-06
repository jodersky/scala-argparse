package argparse

case class arg(
    aliases: Seq[String] = Seq(),
    env: String = null
) extends annotation.StaticAnnotation
