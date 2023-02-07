import argparse.default as ap

@ap.command()
class app():

  @ap.command()
  def version() = println(s"v1000")

  @ap.command()
  class op(factor: Double = 1.0):

    @ap.command()
    def showFactor() = println(s"the factor is $factor")

    @ap.command()
    def multiply(operand: Double) = println(s"result is: ${factor * operand}")

// this is boilerplate for now; it will become obsolete once macro-annotations
// are released
def main(args: Array[String]): Unit = argparse.main(this, args)
