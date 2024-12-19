package com.saldubatech

import io.circe.*
import io.circe.syntax.*
import io.circe.generic.semiauto.*

object Scratch:

  case class Foo(a: Int, b: String)

  case class Bar(a: Int, b: Foo)

end Scratch
