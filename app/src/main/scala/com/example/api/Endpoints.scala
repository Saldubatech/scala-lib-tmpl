package com.example.api

import com.saldubatech.infrastructure.protocols.oas3.types.ResponseEnvelope
import org.example.api.lib.requestresponse.APIError
import org.example.api.lib.requestresponse.APIError.{BadRequest400, Unauthorized401}
import zio.http.endpoint.Endpoint
import zio.http.{int, Header, Method, Route, Routes, Status}
import zio.{Chunk, ZIO}
import zio.http.codec.*
import zio.http.codec.PathCodec.path
import zio.http.endpoint.openapi.{OpenAPIGen, SwaggerUI}
import zio.http.endpoint.openapi.JsonSchema.SchemaStyle
import zio.schema.{DeriveSchema, Schema, TypeId}
import zio.schema.annotation.description

object Endpoints:

  sealed trait Utterance:

    val id: Int
    val content: String

  case class Blah(override val id: Int, override val content: String, tip: String)    extends Utterance
  case class Greeting(override val id: Int, override val content: String, hi: String) extends Utterance

  case class Thingy(
      @description("The Id of the thingy")
      id: Int,
      @description("A unique name for the thingy in this endpoint")
      name: Option[String],
      @description("The nonsense that is uttered by the thingy")
      blahs: List[Utterance])

  case class Th2(id: Int, name: String, traples: List[String])

  given thingyCodec: HttpContentCodec[Thingy] = HttpContentCodec.fromSchema(DeriveSchema.gen[Thingy])
  given th2Codec: HttpContentCodec[Th2]       = HttpContentCodec.fromSchema(DeriveSchema.gen[Th2])

  val crThingy = Endpoint(Method.POST / "thingy").in[Thingy]

  given thingyResponseCodec: ResponseEnvelope.Codec[Thingy] = ResponseEnvelope.codec("ThingyResponse")

  given th2ResponseCodec: HttpContentCodec[ResponseEnvelope[Th2]] = ResponseEnvelope.codec("Th2Response")

  val getThingy = Endpoint(Method.GET / "thingy" / int("thingId")).out[ResponseEnvelope[Thingy]]

  val getThingy2 =
    Endpoint(Method.GET / "thingy" / int("thingId"))
      .out[ResponseEnvelope[Thingy]]
      .header(HeaderCodec.name[Long]("X-Request-Id"))
      .outErrors[APIError](
        HttpCodec.error[BadRequest400](Status.BadRequest),
        HttpCodec.error[Unauthorized401](Status.Unauthorized)
      )

  val getTh2 = Endpoint(Method.GET / "th2" / int("th2Id")).out[ResponseEnvelope[Th2]]

  val getThingyRoute =
    getThingy.implement(id =>
      ZIO.succeed(ResponseEnvelope(id * 100, Thingy(id, Some(s"Thingy[$id]"), List(Blah(id + 1000, "Blah, Blah", "hint")))))
    )

  val getThingy2Route =
    getThingy2.implement(id =>
      ZIO.succeed(
        ResponseEnvelope(id._1 * 100, Thingy(id._1, Some(s"Thingy[$id]"), List(Blah(id._1 + 1000, "Blah, Blah", "hint"))))
      )
    ).toRoutes

  val getTh2Route =
    getTh2.implement(id => ZIO.succeed(ResponseEnvelope(id * 100, Th2(id, s"Th2[$id]", List("Trapple, Trapple")))))

  val getThingyBlahs = Endpoint(Method.GET / "thingy" / int("thingId") / "blahs" / int("blahId"))
    .query(HttpCodec.query[String]("blurb"))
    .out[List[String]]

  val getThingBlahsRoute = getThingyBlahs.implement { case (thingId: Int, blahId: Int, blurb: String) =>
    ZIO.succeed(List(s"Thingy[$thingId][$blahId] is", blurb))
  }

  val openAPI =
    OpenAPIGen.fromEndpoints(title = "Thingy Endpoint Example", version = "1.0.0-SNAPSHOT", getThingy, getTh2, getThingyBlahs)

  val openAPI2 =
    OpenAPIGen.fromEndpoints(title = "Thingy Endpoint Example", version = "1.0.0-SNAPSHOT", referenceType = SchemaStyle.Compact,
      getThingy, getTh2, getThingyBlahs)

  val routes = Routes(getThingyRoute, getTh2Route, getThingBlahsRoute) ++ SwaggerUI.routes("docs" / "openapi", openAPI)

end Endpoints // object
