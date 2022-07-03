package argparse.core

trait VersionSpecificApi extends MainArgsApi {
  types: TypesApi with ParsersApi =>
}
