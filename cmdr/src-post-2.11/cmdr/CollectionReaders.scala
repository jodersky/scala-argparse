package cmdr

import scala.collection.Factory

trait CollectionReaders {

  implicit def CollectionReader[Elem, Col[Elem]](
      implicit elementReader: Reader[Elem],
      factory: Factory[Elem, Col[Elem]]
  ): Reader[Col[Elem]] = new Reader[Col[Elem]] {
    def read(a: String) = {
      val elems: List[Either[String, Elem]] =
        a.split(",").toList.map(elementReader.read(_))
      if (elems.exists(_.isLeft)) {
        val Left(err) = elems.find(_.isLeft).get
        Left(err)
      } else {
        Right(elems.map(_.getOrElse(sys.error("match error"))).to(factory))
      }
    }
  }

}
