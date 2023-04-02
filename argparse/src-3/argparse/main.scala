package argparse

inline def main[Container](instance: Container, args: Iterable[String], env: Map[String, String] = sys.env): Unit = ${
  core.Command.entrypointImpl[Container]('instance, 'args, 'env)
}
