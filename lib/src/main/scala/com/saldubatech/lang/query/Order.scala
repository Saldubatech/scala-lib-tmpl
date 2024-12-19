package com.saldubatech.lang.query

import com.saldubatech.lang.query.Projectable.Step
import com.saldubatech.lang.types.meta.MetaType
import zio.json.{JsonDecoder, JsonEncoder}
import zio.schema.{DeriveSchema, Schema}

enum OrderDirection derives MetaType, JsonEncoder, JsonDecoder:

  case Asc
  case Desc

case class OrderTerm(locator: Projection, direction: OrderDirection) derives MetaType, JsonEncoder, JsonDecoder
object OrderTerm:

end OrderTerm // object

case class Order(terms: List[OrderTerm]) derives MetaType, JsonEncoder, JsonDecoder

object Order:

  import sttp.tapir.json.circe.jsonBody
  // import sttp.tapir.generic.auto.*
  import sttp.tapir.Schema as TSchema

end Order // object
