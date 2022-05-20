object DiffTools {

  // Ignore output mistmatches and overwite expected output with the actual result. This can be
  // helpful after large-scale refactorings.
  // Use it with `OVERWRITE=yes`
  // Don't forget to inspect the resulting diff! Handle with care!
  def overwrite = sys.env.contains("OVERWRITE")

  def assertNoDiff(expected: os.Path, actual: String): Unit =
    if (overwrite) {
      os.write.over(expected, actual)
    } else if (os.read(expected) != actual) {
      val diff =
        os.proc("diff", "--context", expected, os.temp(actual)).call(check = false).out.text()
      throw new java.lang.AssertionError(diff)
    }

}
