package org.example.api.lib.requestresponse

import org.example.api.lib.requestresponse.EntityOperations.PageResult
import org.example.api.lib.types.query.{Page, Query}
import zio.http.codec.*
import zio.http.codec.PathCodec.path
import zio.http.endpoint.Endpoint
import zio.http.{handler, string, Method}
import zio.http.endpoint.openapi.{OpenAPI, OpenAPIGen, SwaggerUI}
import zio.schema.{DeriveSchema, Schema}
import zio.{IO, Tag as ZTag, ZIO, ZNothing}

object EntityOperations:

  case class PageResult[E](thisPage: String, previousPage: String, nextPage: String, results: List[E])

  trait Adaptor[E, S]:

    def handleQuery(q: Query, requestId: Long): IO[APIError, (PageResult[E], Long)]
    def handleFind(q: String, requestId: Long): IO[APIError, (PageResult[E], Long)]
    def getHandler(id: String, requestId: Long): IO[APIError, (E, Long)]
    def createHandler(newE: E, requestId: Long): IO[APIError, (E, Long)]
    def deleteHandler(id: String, requestId: Long): IO[APIError, (E, Long)]
    def updateHandler(eId: String, newE: E, requestId: Long): IO[APIError, (E, Long)]
    def handleQuerySummaries(q: Query, requestId: Long): IO[APIError, (PageResult[S], Long)]
    def handleFindSummaries(q: String, requestId: Long): IO[APIError, (PageResult[S], Long)]

  end Adaptor

  class Implementation[E: ZTag, S: ZTag, A <: Adaptor[E, S]: ZTag](endpoint: EntityOperations[E, S]):

    val queryRoute = endpoint.query
      .implementHandler(handler { (q: Query, requestId: Long) =>
        ZIO.service[A].flatMap(_.handleQuery(q, requestId))
      })
      .toRoutes

    val findRoute =
      endpoint.find
        .implementHandler(handler { (q: String, requestId: Long) =>
          ZIO.service[A].flatMap(_.handleFind(q, requestId))
        })
        .toRoutes

    // val findRoute           = endpoint.find.implementHandler(handler(serviceAdaptor.findHandler)).toRoutes
    val getRoute = endpoint.get
      .implementHandler(handler { (eId: String, requestId: Long) =>
        ZIO.service[A].flatMap(_.getHandler(eId, requestId))
      })
      .toRoutes

    val createRoute = endpoint.create
      .implementHandler(handler { (newE: E, requestId: Long) =>
        ZIO.service[A].flatMap(_.createHandler(newE, requestId))
      })
      .toRoutes

    val deleteRoute = endpoint.delete
      .implementHandler(handler { (eId: String, requestId: Long) =>
        ZIO.service[A].flatMap(_.deleteHandler(eId, requestId))
      })
      .toRoutes

    val updateRoute = endpoint.update
      .implementHandler(handler { (rq: (String, E), requestId: Long) =>
        ZIO.service[A].flatMap(_.updateHandler(rq._1, rq._2, requestId))
      })
      .toRoutes

    val querySummaryRoute = endpoint.querySummaries
      .implementHandler(handler { (q: Query, requestId: Long) =>
        ZIO.service[A].flatMap(_.handleQuerySummaries(q, requestId))
      })
      .toRoutes

    val findSummaryRoute = endpoint.findSummaries
      .implementHandler(handler { (q: String, requestId: Long) =>
        ZIO.service[A].flatMap(_.handleFindSummaries(q, requestId))
      })
      .toRoutes

    def routes =
      queryRoute ++ findRoute ++ getRoute ++ createRoute ++ deleteRoute ++ updateRoute ++ querySummaryRoute ++ findSummaryRoute

    val swaggerRoutes = SwaggerUI.routes("docs" / "openapi" / endpoint.version / endpoint.name, endpoint.openAPI)

  end Implementation

end EntityOperations

trait EntityOperations[E, S]:

  val name: String
  val version: String
  given eSchema: Schema[E]
  given sSchema: Schema[S]

  private given Schema[Query]                        = Query.schema
  private given queryCodec: HttpContentCodec[Query]  = HttpContentCodec.fromSchema(Query.schema)
  private given listCodec: HttpContentCodec[List[E]] = HttpContentCodec.fromSchema(DeriveSchema.gen[List[E]])

  private given pageResultCodec: HttpContentCodec[PageResult[E]] =
    HttpContentCodec.fromSchema(DeriveSchema.gen[EntityOperations.PageResult[E]])

  private given pagResultSummaryCodec: HttpContentCodec[PageResult[S]] =
    HttpContentCodec.fromSchema(DeriveSchema.gen[EntityOperations.PageResult[S]])

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
  ).out[EntityOperations.PageResult[S]](Doc.p(s"List of $name entities that match the query")).decorate

  val get = Endpoint(Method.GET / version / name / string("id")).out[E].decorate

  val create = Endpoint(Method.POST / version / name).in[E].out[E].decorate

  val delete = Endpoint(Method.DELETE / version / name / string("id")).out[E].decorate

  // Endpoint[String, (String, E), ZNothing, E, None]
  val update = Endpoint(Method.PUT / version / name / string("id"))
    .in[E]
    .out[E]
    .decorate

  def openAPI =
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

end EntityOperations // trait
