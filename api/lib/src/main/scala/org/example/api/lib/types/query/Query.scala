package org.example.api.lib.types.query

import zio.json.{DeriveJsonCodec, JsonCodec, JsonDecoder, JsonEncoder}
import zio.schema.{DeriveSchema, Schema}

import org.apache.commons.codec.binary.Base64

import scala.collection.convert.given

object Query:

  val schema          = DeriveSchema.gen[Query]
  given Schema[Query] = schema

  given pageCodec: JsonCodec[Page]     = DeriveJsonCodec.gen[Page]
  given pageEncoder: JsonEncoder[Page] = pageCodec.encoder
  given pageDecoder: JsonDecoder[Page] = pageCodec.decoder

  given projectionCodec: JsonCodec[Projection]     = DeriveJsonCodec.gen[Projection]
  given projectionEncoder: JsonEncoder[Projection] = projectionCodec.encoder
  given projectionDecoder: JsonDecoder[Projection] = projectionCodec.decoder

  given valueCodec: JsonCodec[Filter.Value]     = DeriveJsonCodec.gen[Filter.Value]
  given valueEncoder: JsonEncoder[Filter.Value] = valueCodec.encoder
  given valueDecoder: JsonDecoder[Filter.Value] = valueCodec.decoder

  given intervalCodec: JsonCodec[Filter.Interval]     = DeriveJsonCodec.gen[Filter.Interval]
  given intervalEncoder: JsonEncoder[Filter.Interval] = intervalCodec.encoder
  given intervalDecoder: JsonDecoder[Filter.Interval] = intervalCodec.decoder

  given filterCodec: JsonCodec[Filter]     = DeriveJsonCodec.gen[Filter]
  given filterEncoder: JsonEncoder[Filter] = filterCodec.encoder
  given filterDecoder: JsonDecoder[Filter] = filterCodec.decoder

  given orderDirectionCodec: JsonCodec[OrderDirection]     = DeriveJsonCodec.gen[OrderDirection]
  given orderDirectionEncoder: JsonEncoder[OrderDirection] = orderDirectionCodec.encoder
  given orderDirectionDecoder: JsonDecoder[OrderDirection] = orderDirectionCodec.decoder

  given orderTermCodec: JsonCodec[OrderTerm]     = DeriveJsonCodec.gen[OrderTerm]
  given orderTermEncoder: JsonEncoder[OrderTerm] = orderTermCodec.encoder
  given orderTermDecoder: JsonDecoder[OrderTerm] = orderTermCodec.decoder

  given orderCodec: JsonCodec[Order]     = DeriveJsonCodec.gen[Order]
  given orderEncoder: JsonEncoder[Order] = orderCodec.encoder
  given orderDecoder: JsonDecoder[Order] = orderCodec.decoder

  val jsonCodec = DeriveJsonCodec.gen[Query]

  def fromJson(json: String) = jsonCodec.decodeJson(new String(Base64.decodeBase64(json)))

end Query // object

case class Query(
    page: Page = Page.default,
    filter: Option[Filter] = None,
    order: Option[Order] = None):

  def toJson: String = Base64.encodeBase64String(Query.jsonCodec.encodeJson(this, None).toString.getBytes)
end Query // class
