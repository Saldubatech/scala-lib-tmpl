package com.example.api

import zio.http.endpoint.Endpoint
import zio.http.{int, Method, Routes}
import zio.ZIO
import zio.http.codec.HttpCodec
import zio.http.codec.*
import zio.http.codec.PathCodec.path
import zio.http.endpoint.openapi.{OpenAPIGen, SwaggerUI}
import zio.schema.DeriveSchema

object Endpoints:

  sealed trait Utterance:

    val id: Int
    val content: String

  case class Blah(override val id: Int, override val content: String, tip: String)    extends Utterance
  case class Greeting(override val id: Int, override val content: String, hi: String) extends Utterance
  case class Thingy(id: Int, name: String, blahs: List[Utterance])

  given HttpContentCodec[Thingy] = HttpContentCodec.fromSchema(DeriveSchema.gen[Thingy])

  val getThingy = Endpoint(Method.GET / "thingy" / int("thingId")).out[Thingy]

  val getThingyRoute =
    getThingy.implement(id => ZIO.succeed(Thingy(id, s"Thingy[$id]", List(Blah(id + 1000, "Blah, Blah", "hint")))))

  val getThingyBlahs = Endpoint(Method.GET / "thingy" / int("thingId") / "blahs" / int("blahId"))
    .query(HttpCodec.query[String]("blurb"))
    .out[List[String]]

  val getThingBlahsRoute = getThingyBlahs.implement { case (thingId: Int, blahId: Int, blurb: String) =>
    ZIO.succeed(List(s"Thingy[$thingId][$blahId] is", blurb))
  }

  val openAPI = OpenAPIGen.fromEndpoints(title = "Thingy Endpoint Example", version = "1.0.0-SNAPSHOT", getThingy, getThingyBlahs)
  val routes  = Routes(getThingyRoute, getThingBlahsRoute) ++ SwaggerUI.routes("docs" / "openapi", openAPI)

end Endpoints // object
