package com.saldubatech.lang.query

import com.saldubatech.lang.types.AppResult
import com.saldubatech.lang.types.meta.MetaType
import zio.schema.{DeriveSchema, Schema}
import org.apache.commons.codec.binary.Base64

import scala.collection.convert.given
import ValueType.given
import zio.json.*

object Query:

  def decodeUrl(encoded: String): String = new String(Base64.decodeBase64(encoded))
  private val decoder                    = summon[JsonDecoder[Query]]

  def fromJson(json: String)(using qDt: MetaType[Query]): AppResult[Query] =
    decoder.decodeJson(decodeUrl(json)).left.map(err => AppResult.Error("Json Decoding Error: $err"))

//    qDt.decodeJson(decodeUrl(json).asJson).left.map(err => AppResult.Error("Json Decoding Error", Some(err)))

//implicit def queryJsonEncoder: JsonEncoder[Query] = DeriveJsonEncoder.gen[Query]

end Query // object

case class Query(
    page: Page = Page(),
    filter: Option[Filter] = None,
    order: Option[Order] = None)
    derives MetaType,
      JsonEncoder,
      JsonDecoder:

  private val encoder = summon[JsonEncoder[Query]]
  def toJson: String  = encoder.encodeJson(this).toString
  def encoded: String = Base64.encodeBase64String(toJson.getBytes)

  def previousPage: Query = copy(page = page.previous)

  def nextPage: Query = copy(page = page.next)

end Query // class
