import utest.*

import argparse.core.DocComment

object DocCommentTest extends TestSuite:

  val tests = Tests{
    test("basic") {
      DocComment.extract("/** hello world */").paragraphs ==> Seq("hello world")
      DocComment.extract("/** hello world   */").paragraphs ==> Seq("hello world")
      DocComment.extract("/**   hello world   */").paragraphs ==> Seq("hello world")
      DocComment.extract("/**hello world*/").paragraphs ==> Seq("hello world")
    }
    test("paragraphs") {
      DocComment.extract(
        """|/** hello world
           |*/""".stripMargin
      ).paragraphs ==> Seq("hello world")
      DocComment.extract(
        """|/** hello world
           |  */""".stripMargin
      ).paragraphs ==> Seq("hello world")
      DocComment.extract(
        """|/** hello world
           |
           |*/""".stripMargin
      ).paragraphs ==> Seq("hello world")
      DocComment.extract(
        """|/** hello world
           |*
           |*/""".stripMargin
      ).paragraphs ==> Seq("hello world")
      DocComment.extract(
        """|/** hello world
           |  *
           |  */""".stripMargin
      ).paragraphs ==> Seq("hello world")
      DocComment.extract(
        """|/**
           |  * hello world
           |  *
           |  */""".stripMargin
      ).paragraphs ==> Seq("hello world")
      DocComment.extract(
        """|/**
           | * hello world
           | *
           | */""".stripMargin
      ).paragraphs ==> Seq("hello world")
      DocComment.extract(
        """|/**
           | * hello world */""".stripMargin
      ).paragraphs ==> Seq("hello world")
      DocComment.extract(
        """|/**
           |  * hello
           |  * world
           |  *
           |  */""".stripMargin
      ).paragraphs ==> Seq("hello world")
    }
    test("paragraphs2") {
      DocComment.extract(
        """|/**
           |  * hello
           |  *
           |  * world
           |  *
           |  */""".stripMargin
      ).paragraphs ==> Seq("hello", "world")
      DocComment.extract(
        """|/**
           |  * hello
           |  *
           |  *
           |  *
           |  * world
           |  *
           |  */""".stripMargin
      ).paragraphs ==> Seq("hello", "world")
      DocComment.extract(
        """|/** Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a
           |  * nulla odio. Aliquam diam ex, consequat vitae condimentum vitae,
           |  * cursus non ex. Suspendisse vehicula efficitur augue.
           |  *
           |  * Curabitur viverra blandit turpis non fringilla. Phasellus sit
           |  * amet turpis non nibh aliquet varius quis ut metus.
           |  *
           |  * Curabitur viverra blandit turpis non fringilla. Phasellus sit
           |  * amet turpis non nibh aliquet varius quis ut metus.
           |  */""".stripMargin
      ).paragraphs ==> Seq(
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a nulla odio. Aliquam diam ex, consequat vitae condimentum vitae, cursus non ex. Suspendisse vehicula efficitur augue.",
        "Curabitur viverra blandit turpis non fringilla. Phasellus sit amet turpis non nibh aliquet varius quis ut metus.",
        "Curabitur viverra blandit turpis non fringilla. Phasellus sit amet turpis non nibh aliquet varius quis ut metus."
      )
    }
    test("params") {
      DocComment.extract("/** @param x hello world */").params ==> Map("x" -> "hello world")
      DocComment.extract("/** @param x\nhello world */").params ==> Map("x" -> "hello world")
      DocComment.extract("/** @param x\n  hello world */").params ==> Map("x" -> "hello world")
      DocComment.extract("/** @param x\n  hello \n\n@param y world*/").params ==> Map("x" -> "hello", "y" -> "world")
    }
    test("full") {
      val c = DocComment.extract(
        """|/** Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a
           |  * nulla odio. Aliquam diam ex, consequat vitae condimentum vitae,
           |  * cursus non ex. Suspendisse vehicula efficitur augue.
           |  *
           |  * Curabitur viverra blandit turpis non fringilla. Phasellus sit
           |  * amet turpis non nibh aliquet varius quis ut metus.
           |  *
           |  * @param param1 Curabitur viverra blandit turpis non fringilla. Phasellus sit
           |  * amet turpis non nibh aliquet varius quis ut metus.
           |  *
           |  * @param param2 Curabitur viverra blandit turpis non fringilla. Phasellus sit
           |  * amet turpis non nibh aliquet varius quis ut metus.
           |  */""".stripMargin
      )
      c.paragraphs ==> Seq(
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec a nulla odio. Aliquam diam ex, consequat vitae condimentum vitae, cursus non ex. Suspendisse vehicula efficitur augue.",
        "Curabitur viverra blandit turpis non fringilla. Phasellus sit amet turpis non nibh aliquet varius quis ut metus."
      )
      c.params ==> Map(
        "param1" -> "Curabitur viverra blandit turpis non fringilla. Phasellus sit amet turpis non nibh aliquet varius quis ut metus.",
        "param2" -> "Curabitur viverra blandit turpis non fringilla. Phasellus sit amet turpis non nibh aliquet varius quis ut metus."
      )
    }
  }
