package com.saldubatech.lang.query

//import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}
//import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import com.saldubatech.lang.types.meta.MetaType
import zio.json.{JsonDecoder, JsonEncoder}
import zio.schema.{DeriveSchema, Schema}

case class Projection(path: Projectable.Locator) derives JsonEncoder, JsonDecoder

object Projection:
  // Need special encoding to avoid too much nesting
  given projectionStepSchema: Schema[Projection] =
    DeriveSchema
      .gen[List[Projectable.Step]]
      .transform[Projection](
        path => Projection(path),
        prj => prj.path
      )

  implicit def p(path: Projectable.Locator): Projection = Projection(path)

  def apply(path: Projectable.Step*): Projection = Projection(path.toList)

end Projection // object
