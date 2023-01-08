import argparse.default as ap

@ap.command()
def main(
  num: Int = 0,
  num2: Double = 0,
  path: os.Path = os.pwd, // relative paths on the command line will be resolved to absolute paths w.r.t. to pwd
  keyValue: (String, Int) = ("a" -> 2),
  keyValues: Seq[(String, Int)] = Seq()
) =
  println(s"num=$num")
  println(s"num2=$num2")
  println(s"path=$path")
  println(s"keyValue=$keyValue")
  println(s"keyValues=$keyValues")

// boilerplate until Scala 3 supports macro annotations
def main(args: Array[String]) = argparse.main(this, args)
