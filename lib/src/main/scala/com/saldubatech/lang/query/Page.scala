package com.saldubatech.lang.query

import com.saldubatech.lang.types.meta.MetaType
import zio.json.{JsonDecoder, JsonEncoder}
import zio.schema.{DeriveSchema, Schema}

object Page:

  inline def first(inline p: Page): Int = p.size * p.n

  inline def n(inline n: Int = 0, inline size: Int = 10): Page = Page(n * size, (n + 1) * size - 1)

end Page // object

// Base 0 counting
case class Page(first: Int = 0, last: Int = 9) derives MetaType, JsonEncoder, JsonDecoder:

  // To be friendly to Macros.
  inline def size: Int = last - first + 1
  inline def n: Int    = first / size

  val sizeVal = size
  val nVal    = n

  def previous: Page = if n == 0 then Page(first - size, last - size) else this

  def next: Page = Page(first + size, last + size)

end Page // class
