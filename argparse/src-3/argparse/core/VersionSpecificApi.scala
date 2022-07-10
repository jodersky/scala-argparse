package argparse.core

trait VersionSpecificParsersApi extends MainArgsApi {
  types: TypesApi with ParsersApi =>

  trait ParserExtra { self: ArgumentParser =>

    /** Add commands declared via main annotations within a container. */
    inline def commands[A](mk: => A): Unit = {
      val mains = findMains[A]
      mains.foreach{ ep =>
        self.command(ep.name, a => ep.invoke(mk)(a))
      }
    }

  }
}
