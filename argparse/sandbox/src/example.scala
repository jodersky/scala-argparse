// A sandbox to experiment with argarse

object api
  extends argparse.core.ParsersApi
  with argparse.core.MacroApi
  with argparse.core.TypesApi
  with argparse.core.ReadersApi:

  override def defaultHelpFlags = Seq("-h")

/**
  * Hello world
  *
  * @param base the base value
  */
@api.command()
class wrapper(base: Int):
  @api.command()
  def add(x: Int) = println(base + x)

  /** A nested command */
  @api.command()
  def nested(y: Int = 2) = foo(y)
  class foo(y: Int):
    @api.command()
    def ok() = println("ok")

def main(args: Array[String]): Unit = argparse.main(this, args)
