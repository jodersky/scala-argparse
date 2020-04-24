object Main {

  // this shows how to manually use the argparser API
  def main(args: Array[String]) = {
    val parser = cmdr.ArgumentParser()
    val port = parser.param("--port", 2222, env = "EXAMPLE_PORT", aliases=Seq("-p"))
    val path1 = parser.param("--path1", os.pwd)
    val path2 = parser.requiredParam[os.SubPath]("--path2")
    parser.parse(args)
    println(port.get)
    println(path1.get)
    println(path2.get)
  }

}
