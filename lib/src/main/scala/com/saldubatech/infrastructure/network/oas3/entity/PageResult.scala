package com.saldubatech.infrastructure.network.oas3.entity

import com.saldubatech.lang.types.meta.MetaType

case class PageResult[E](
    thisPage: String,
    previousPage: String,
    nextPage: String,
    results: List[EntityResult[E]])
    derives MetaType:

  def mapResults[RS: MetaType](f: EntityResult[E] => EntityResult[RS]): PageResult[RS] = copy(results = results.map(f))

end PageResult

object PageResult:

end PageResult
