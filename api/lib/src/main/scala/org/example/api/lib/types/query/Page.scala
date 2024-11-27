package org.example.api.lib.types.query

object Page:
  val default = Page(1, 10)
end Page // object

case class Page(n: Int, size: Int)
