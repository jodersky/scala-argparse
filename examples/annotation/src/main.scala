import argparse.default as ap

/** This is an example app.
  *
  * It does something interesting.
  *
  * @param p1 description goes here
  *
  * @param p2 description goes here
  */
@ap.main()
def foo(p1: String = "a", p2: Int) = ()

def main(args: Array[String]) = ap.dispatch(this, args)
