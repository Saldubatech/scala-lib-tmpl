package com.saldubatech.infrastructure.protocols.oas3.types

import zio.http.codec.HttpContentCodec
import zio.schema.{DeriveSchema, Schema, TypeId}

object ResponseEnvelope:

  type Codec[P] = HttpContentCodec[ResponseEnvelope[P]]

  def codec[P](using schP: Schema[P]): Codec[P] = HttpContentCodec.fromSchema(DeriveSchema.gen[ResponseEnvelope[P]])

  inline def codec[P]: Codec[P] = HttpContentCodec.fromSchema(DeriveSchema.gen[ResponseEnvelope[P]])

  inline def codec[P](tn: String): Codec[P] = codec(DeriveSchema.gen[P], tn)

  def codec[P](schP: Schema[P], tn: String): Codec[P] =
    val sch: Schema[ResponseEnvelope[P]] = Schema.CaseClass2[Int, P, ResponseEnvelope[P]](
      id0 = TypeId.fromTypeName(tn),
      field01 = Schema.Field(name0 = "id", schema0 = Schema[Int], get0 = _.id, set0 = (p, x) => p.copy(id = x)),
      field02 = Schema.Field(name0 = "payload", schema0 = schP, get0 = _.payload, set0 = (p, x) => p.copy(payload = x)),
      construct0 = (id, payload) => ResponseEnvelope(id, payload)
    )
    HttpContentCodec.fromSchema(sch)

end ResponseEnvelope // object

case class ResponseEnvelope[P](id: Int, payload: P)
