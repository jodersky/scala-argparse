package argparse

inline def main[Container](instance: Container, args: Iterable[String], env: Map[String, String] = sys.env): Unit = ${
  core.CommandMacros.mainImpl[Container]('instance, 'args, 'env)
}
