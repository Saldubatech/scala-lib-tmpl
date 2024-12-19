package com.saldubatech.infrastructure.network.oas3.entity

import com.saldubatech.infrastructure.network.oas3.Adaptor.decorate
import com.saldubatech.lang.query.{Page, Query}
import com.saldubatech.lang.types.meta.MetaType
import zio.Tag as ZTag
import zio.http.codec.*
import zio.http.endpoint.Endpoint
import zio.http.endpoint.openapi.{OpenAPI, OpenAPIGen}
import zio.http.{string, Method}
import zio.schema.{DeriveSchema, Schema}

trait EntityEndpoint[E: ZTag: EntityResult.API: MetaType, S: ZTag: EntityResult.API: MetaType]:

  val name: String
  val version: String
  val eDT = summon[MetaType[E]]
  val sDT = summon[MetaType[S]]

  val eSchema: Schema[E] = eDT.zioSchema
  val sSchema: Schema[S] = sDT.zioSchema

  val eEDT: MetaType[EntityResult[E]] = summon[MetaType[EntityResult[E]]]
  val sEDT: MetaType[EntityResult[S]] = summon[MetaType[EntityResult[S]]]

  given eESchema: Schema[EntityResult[E]] = eEDT.zioSchema
  given eSSchema: Schema[EntityResult[S]] = sEDT.zioSchema

  private given eCodec: HttpContentCodec[E]                = HttpContentCodec.fromSchema(eSchema)
  private given sCodec: HttpContentCodec[S]                = HttpContentCodec.fromSchema(sSchema)
  private given eECodec: HttpContentCodec[EntityResult[E]] = HttpContentCodec.fromSchema(eESchema)
  private given sECodec: HttpContentCodec[EntityResult[S]] = HttpContentCodec.fromSchema(eSSchema)

  val queryDT: MetaType[Query] = summon[MetaType[Query]]
  val querySchema              = queryDT.zioSchema

  private given queryCodec: HttpContentCodec[Query] = HttpContentCodec.fromSchema(querySchema)

  given pageSchema: Schema[PageResult[E]]                        = DeriveSchema.gen[PageResult[E]]
  private given pageResultCodec: HttpContentCodec[PageResult[E]] = HttpContentCodec.fromSchema(pageSchema)

  private given pagResultSummaryCodec: HttpContentCodec[PageResult[S]] =
    HttpContentCodec.fromSchema(DeriveSchema.gen[PageResult[S]])

  val query = Endpoint((Method.POST / version / name / "query") ?? Doc.p(s"Start a Query for $name entities"))
    .in[Query]
    .out[PageResult[E]]
    .decorate

  val find =
    Endpoint((Method.GET / version / name / "query" / string("pageId")) ?? Doc.p(s"Find $name entities with a query parameter"))
      .out[PageResult[E]](Doc.p(s"List of $name entities that match the query"))
      .decorate

  val querySummaries = Endpoint(
    (Method.POST / version / name / "query" / "summary") ?? Doc.p(s"Start a Query for summaries of $name entities")
  )
    .in[Query]
    .out[PageResult[S]]
    .decorate

  val findSummaries = Endpoint(
    (Method.GET / version / name / "query" / "summary" / string("pageId")) ??
      Doc.p(s"Find $name summaries with a page identifier")
  ).out[PageResult[S]](Doc.p(s"List of $name entities that match the query")).decorate

  val get = Endpoint(Method.GET / version / name / string("id")).out[EntityResult[E]].decorate

  val create = Endpoint(Method.POST / version / name).in[E].out[EntityResult[E]].decorate

  val delete = Endpoint(Method.DELETE / version / name / string("id")).out[EntityResult[E]].decorate

  // Endpoint[String, (String, E), ZNothing, E, None]
  type TenantEntity = EntityResult[E]

  val update = Endpoint(Method.PUT / version / name / string("id"))
    .in[E]
    .out[TenantEntity](Doc.p(s"The new value for the Tenant Entity"))
    .decorate

  def openAPI: OpenAPI =
    OpenAPIGen.fromEndpoints(
      s"Entity $name Endpoint",
      version = version,
      query,
      get,
      find,
      create,
      delete,
      update,
      querySummaries,
      findSummaries
    )

end EntityEndpoint
