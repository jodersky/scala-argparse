package argparse.core

trait MacroApi extends TypesApi with ParsersApi with OutputApi:

  // this will be used once macro-annotations are released
  // class main() extends annotation.StaticAnnotation

  class command() extends annotation.StaticAnnotation
  class unknownCommand() extends annotation.StaticAnnotation

  // these annotations are not bound to a specific API bundle, but we add
  // forwarders for convenience
  type name = argparse.name
  val name = argparse.name
  type alias = argparse.alias
  val alias = argparse.alias
  type env = argparse.env
  val env = argparse.env
