package com.saldubatech.domain.types.geo

import com.saldubatech.lang.types.meta.MetaType
import io.circe.Codec
import zio.schema.{derived, DeriveSchema, Schema}

object Address:

end Address

case class Address(
    firstLine: String,
    secondLine: Option[String],
    city: String,
    region: String,
    postalCode: String,
    country: String)
    derives Schema,
      Codec
