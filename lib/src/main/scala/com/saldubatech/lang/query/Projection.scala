package com.saldubatech.lang.query

//import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}
//import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import com.saldubatech.lang.query.Projectable.{locatorEncoder, Field, Index}
import com.saldubatech.lang.types.meta.MetaType
import com.saldubatech.lang.types.AppResult
import zio.json.{JsonDecoder, JsonEncoder}
import zio.schema.{DeriveSchema, Schema}

case class Projection(path: Projectable.Locator)

object Projection:

  import Projectable.*
  import Projectable.given
  import Projectable.Step.given

  // Need special encoding to avoid too much nesting in the Json representation
  given schema: Schema[Projection] =
    Projectable.locatorSchema.transform[Projection](
      path => Projection(path),
      prj => prj.path
    )

  given projectionJsonEncoder: JsonEncoder[Projection] =
    Projectable.locatorEncoder.contramap[Projection](prj =>
      println(s"### Encoding Projection to Locator $prj")
      prj.path
    )

  given projectionJsonDecoder: JsonDecoder[Projection] =
    Projectable.locatorDecoder.map[Projection]((l: Projectable.Locator) =>
      println(s"##### Decoding from Locator to Projection $l")
      Projection(l)
    )

  implicit def p(path: Projectable.Locator): Projection = Projection(path)

  def apply(path: Projectable.Step*): Projection = Projection(path.toList)

end Projection // object
