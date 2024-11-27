package com.saldubatech.infrastructure.protocols.oas3.types

sealed trait OrderDirection
case object Asc  extends OrderDirection
case object Desc extends OrderDirection

case class OrderTerm(locator: String, direction: OrderDirection)
case class Order(o: OrderTerm, rest: OrderTerm*)
