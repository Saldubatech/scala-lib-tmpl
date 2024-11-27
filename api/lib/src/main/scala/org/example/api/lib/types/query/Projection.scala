package org.example.api.lib.types.query

case class Projection(path: String)

object Projection:

  def apply(path: Iterable[String]): Projection = Projection(path.mkString("."))

end Projection // object
