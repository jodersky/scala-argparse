package cmdr

import scala.collection.generic.CanBuildFrom

trait CollectionReaders {

  implicit def CollectionReader[Elem, Col[Elem]](
      implicit elementReader: Reader[Elem],
      factory: CanBuildFrom[Nothing, Elem, Col[Elem]]
  ): Reader[Col[Elem]] = new Reader[Col[Elem]] {
    def read(a: String) = {
      val elems: List[Either[String, Elem]] =
        a.split(",").toList.map(elementReader.read(_))
      if (elems.exists(_.isLeft)) {
        val Left(err) = elems.find(_.isLeft).get
        Left(err)
      } else {
        Right(elems.map(_.right.get).to(factory))
      }
    }
  }

}
