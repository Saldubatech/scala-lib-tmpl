package org.example.api.lib.types.query

sealed trait OrderDirection
case object Asc  extends OrderDirection
case object Desc extends OrderDirection

case class OrderTerm(locator: String, direction: OrderDirection)
case class Order(terms: List[OrderTerm])
