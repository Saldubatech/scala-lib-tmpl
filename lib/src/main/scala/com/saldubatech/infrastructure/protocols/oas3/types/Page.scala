package com.saldubatech.infrastructure.protocols.oas3.types

object Page:
  val default = Page(1, 10)
end Page // object

case class Page(n: Int, size: Int)
