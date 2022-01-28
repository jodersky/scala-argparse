package argparse

object PredefReader {

  def read(cfg: String): Seq[String] = {
    val b = collection.mutable.ListBuffer.empty[String]
    cfg.linesIterator.foreach{ line =>
      val trimmed = line.trim()
      if (!trimmed.startsWith("#") && !trimmed.isEmpty()) {
        val parts = ("--" + trimmed).split("""\s+""", 2)
        b ++= parts
      }
    }
    b.result()
  }

}
