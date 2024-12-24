package com.saldubatech.infrastructure.network.oas3.entity

import com.saldubatech.lang.query.{Filter, Order, Page, Projectable, Projection, Query, ValueType}
import com.saldubatech.lang.query.Projectable.{Field, Index, Step}
import zio.http.codec.*

object QueryContentCodecs:

  import com.saldubatech.lang.types.meta.MetaType.given
  import ValueType.given

  given valueTypeCodec: HttpContentCodec[ValueType.VALUE] = HttpContentCodec.fromSchema[ValueType.VALUE]

  given fieldCodec: HttpContentCodec[Field]                 = HttpContentCodec.fromSchema[Field]
  given indexCodec: HttpContentCodec[Index]                 = HttpContentCodec.fromSchema[Index]
  given stepCodec: HttpContentCodec[Step]                   = HttpContentCodec.fromSchema[Step]
  given locatorCodec: HttpContentCodec[Projectable.Locator] = HttpContentCodec.fromSchema[Projectable.Locator]
  given projectionCodec: HttpContentCodec[Projection]       = HttpContentCodec.fromSchema[Projection]

  given filterCodec: HttpContentCodec[Filter] = HttpContentCodec.fromSchema[Filter]
  given pageCodec: HttpContentCodec[Page]     = HttpContentCodec.fromSchema[Page]
  given orderCodec: HttpContentCodec[Order]   = HttpContentCodec.fromSchema[Order]
  given queryCodec: HttpContentCodec[Query]   = HttpContentCodec.fromSchema[Query]

end QueryContentCodecs
