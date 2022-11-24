package configparse

trait ArgparseParams extends argparse.core.MainArgsApi with configparse.core.SettingApi:

  given [S](
    using root: SettingRoot[S],
    pr: Reader[os.Path]
  ): ParamMaker[S] with
    override def makeParams(name: String, default: Option[() => S], annot: arg, argparser: ArgumentParser): () => S =
      require(default.isDefined, "configuration parameters must have a default value")
      val instance = default.get()

      val configFiles = argparser.repeatedParam[os.Path](
        s"--$name",
        aliases = annot.aliases,
        help = annot.doc,
        flag = false,
        endOfNamed = false
      )
      argparser.postCheck( (args, env) =>
        read(instance, configFiles.value, env = env) match
          case true => None
          case false => Some("error reading configuration files")
      )
      () => instance
