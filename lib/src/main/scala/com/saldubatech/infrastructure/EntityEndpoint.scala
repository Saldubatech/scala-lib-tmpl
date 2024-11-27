package com.saldubatech.infrastructure

import com.saldubatech.infrastructure.protocols.oas3.types.ResponseEnvelope
import zio.http.endpoint.Endpoint
import zio.http.{int, string, Method}
import zio.http.codec.*
import zio.http.codec.PathCodec.path
import zio.http.endpoint.openapi.{OpenAPIGen, SwaggerUI}
import zio.schema.{DeriveSchema, Schema}

object EntityEndpoint:

end EntityEndpoint // object

class EntityEndpoint[E, Q](name: String, version: String)(using eSchema: Schema[E], qSchema: Schema[Q]):

  private given listCodec: HttpContentCodec[List[E]]      = HttpContentCodec.fromSchema(DeriveSchema.gen[List[E]])
  private given eResponseCodec: ResponseEnvelope.Codec[E] = ResponseEnvelope.codec(eSchema, s"${name}Response")

  val find = Endpoint((Method.GET / name) ?? Doc.p("Find $name entities with a query parameter"))
    .query(HttpCodec.queryAll[Q] ?? Doc.p("Query Parameter for filtering $name entities"))
    .out[List[E]](Doc.p("List of $name entities that match the query"))

  val get    = Endpoint(Method.GET / name / string("id")).out[ResponseEnvelope[E]]
  val create = Endpoint(Method.POST / name).in[E]
  val delete = Endpoint(Method.DELETE / name / string("id")).out[ResponseEnvelope[E]]
  val update = Endpoint(Method.PUT / name / string("id")).in[E]

  val openAPI  = OpenAPIGen.fromEndpoints(s"Entity $name Endpoint", version = version, get, find, create, delete, update)
  val oasRoute = SwaggerUI.routes(path = "docs" / "openapi" / s"entity-$name", openAPI)

end EntityEndpoint // class
