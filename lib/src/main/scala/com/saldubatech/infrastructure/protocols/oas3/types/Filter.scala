package com.saldubatech.infrastructure.protocols.oas3.types

sealed trait Filter

object Filter:

  case class And(left: Filter, right: Filter, rest: Filter*) extends Filter
  case class Or(left: Filter, right: Filter, rest: Filter*)  extends Filter
  case class Not(filter: Filter)                             extends Filter

end Filter
