package com.saldubatech.lang.query

import zio.json.{JsonDecoder, JsonEncoder}
import zio.schema.Schema

case class Projection(path: Projectable.Locator)

object Projection:

  import Projectable.{*, given}

  // Need special encoding to avoid too much nesting in the Json representation
  given schema: Schema[Projection] =
    Projectable.locatorSchema.transform[Projection](
      path => Projection(path),
      prj => prj.path
    )

  given projectionJsonEncoder: JsonEncoder[Projection] = Projectable.locatorEncoder.contramap[Projection](prj => prj.path)

  given projectionJsonDecoder: JsonDecoder[Projection] =
    Projectable.locatorDecoder.map[Projection]((l: Projectable.Locator) => Projection(l))

  implicit def p(path: Projectable.Locator): Projection = Projection(path)

  def apply(path: Projectable.Step*): Projection = Projection(path.toList)

end Projection // object
