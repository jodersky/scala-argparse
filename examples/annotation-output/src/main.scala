import argparse.default as ap

case class Item(
  name: String,
  value: Double,
  comment: String
)

@ap.command()
def main() =
  List(
    Item("item1", 2, ""),
    Item("item2", 0.213, "notice the numeric alignment"),
    Item("item3", -100.2, ""),
    Item("item4", 10.2, "a comment"),
    Item("another_item", 12.54, "a comment"),
    Item("", 12.54, "item has no name"),
    Item("etc", 0, "...")
  )

// boilerplate necessary until macro annotations become available in Scala 3
def main(args: Array[String]): Unit = argparse.main(this, args)
