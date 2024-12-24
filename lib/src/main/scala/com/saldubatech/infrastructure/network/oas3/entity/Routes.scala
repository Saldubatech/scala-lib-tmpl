package com.saldubatech.infrastructure.network.oas3.entity

import com.saldubatech.infrastructure.services.Entity
import com.saldubatech.lang.query.Query
import zio.{ZIO, Tag as ZTag}
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.path
import zio.http.endpoint.openapi.SwaggerUI

class Routes[E <: Entity: ZTag: EntityResult.API, S: ZTag: EntityResult.API, A <: Adaptor[E, S]: ZTag](
    val endpoint: EntityEndpoint[E, S]
  )(using summarizer: Option[Adaptor.Summarizer[E, S]] = None):

  private val queryRoute = endpoint.query
    .implementHandler(handler { (q: Query, requestId: Long) =>
      ZIO.service[A].flatMap(_.handleQuery(q, requestId))
    })
    .toRoutes

  private val findRoute =
    endpoint.find
      .implementHandler(handler { (q: String, requestId: Long) =>
        ZIO.service[A].flatMap(_.handleFind(q, requestId))
      })
      .toRoutes

  private val getRoute = endpoint.get
    .implementHandler(handler((eId: String, requestId: Long) => ZIO.service[A].flatMap(a => a.handleGet(eId, requestId))))
    .toRoutes

  private val createRoute = endpoint.create
    .implementHandler(handler((newE: E, requestId: Long) => ZIO.service[A].flatMap(_.handleCreate(newE, requestId))))
    .toRoutes

  private val deleteRoute = endpoint.delete
    .implementHandler(handler((eId: String, requestId: Long) => ZIO.service[A].flatMap(_.handleDelete(eId, requestId))))
    .toRoutes

  private val updateRoute = endpoint.update
    .implementHandler(handler { (rq: (String, E), requestId: Long) =>
      ZIO.service[A].flatMap(_.handleUpdate(rq._1, rq._2, requestId))
    })
    .toRoutes

  private lazy val appRoutes = summarizer match
    case None =>
      queryRoute ++ findRoute ++ getRoute ++ createRoute ++ deleteRoute ++ updateRoute
    case Some(s) =>
      given Adaptor.Summarizer[E, S] = s

      val querySummaryRoute = endpoint.querySummaries
        .implementHandler(handler((q: Query, requestId: Long) => ZIO.service[A].flatMap(_.handleQuerySummaries(q, requestId))))
        .toRoutes

      val findSummaryRoute = endpoint.findSummaries
        .implementHandler(handler((q: String, requestId: Long) => ZIO.service[A].flatMap(_.handleFindSummaries(q, requestId))))
        .toRoutes
      queryRoute ++ findRoute ++ getRoute ++ createRoute ++ deleteRoute ++ updateRoute ++ querySummaryRoute ++ findSummaryRoute

  val swaggerRoutes = SwaggerUI.routes("docs" / "openapi" / endpoint.version / endpoint.name, endpoint.openAPI)

  lazy val routes = appRoutes ++ swaggerRoutes
