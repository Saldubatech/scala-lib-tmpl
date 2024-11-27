package org.example.api.lib.requestresponse

import com.saldubatech.lang.types.AppResult
import com.saldubatech.lang.types.datetime.Epoch
import org.example.api.lib.bindings.ServiceEndpoint
import org.example.api.lib.types.UUID
import zio.{IO, Tag as ZTag, ZIO, ZNothing}
import zio.http.{handler, Handler, Header, Method, Route, RoutePattern, Routes, Status}
import zio.http.codec.{Doc, HttpCodec}
import zio.http.endpoint.Endpoint
import zio.http.endpoint.openapi.OpenAPIGen
import zio.http.endpoint.AuthType.None
import zio.schema.{DeriveSchema, Schema}

object Subject:

  case class SubscriptionId(home: ServiceEndpoint, id: UUID)
  val subscriptionIdSchema = DeriveSchema.gen[SubscriptionId]

  trait Adaptor:

    val forEndpoint: ServiceEndpoint
    val subscribe: ServiceEndpoint => IO[AppResult.Error, Subject.SubscriptionId]
    val unsubscribe: Subject.SubscriptionId => IO[AppResult.Error, Boolean]

    private val errorHandler = APIError.Mapper(forEndpoint)

    def handleSubscribe(locator: ServiceEndpoint, requestId: Long): IO[APIError, (Subject.SubscriptionId, Long)] =
      subscribe(locator).mapBoth(errorHandler.map(requestId, Epoch.now, _), subscription => (subscription, requestId))

    def handleUnsubscribe(sId: Subject.SubscriptionId, requestId: Long): IO[APIError, (Boolean, Long)] =
      unsubscribe(sId).mapBoth(errorHandler.map(requestId, Epoch.now, _), success => (success, requestId))

  end Adaptor

  trait Implementation[A <: Adaptor: ZTag](endpoint: Subject):

    val subscribeRoute = endpoint.subscribe
      .implementHandler(handler { (locator: ServiceEndpoint, rqId: Long) =>
        ZIO.service[A].flatMap(_.handleSubscribe(locator, rqId))
      })
      .toRoutes

    val unsubscribeRoute =
      endpoint.unsubscribe
        .implementHandler(handler { (sId: Subject.SubscriptionId, rqId: Long) =>
          ZIO.service[A].flatMap(_.handleUnsubscribe(sId, rqId))
        })
        .toRoutes

    val subjectRoutes = subscribeRoute ++ unsubscribeRoute

  end Implementation

end Subject

trait Subject:
  self: EntityOperations[?, ?] =>

  given Schema[ServiceEndpoint]        = ServiceEndpoint.schema
  given Schema[Subject.SubscriptionId] = Subject.subscriptionIdSchema

  val routePath: RoutePattern[Unit] = Method.POST / self.version / self.name / "subscribe"

  val subscribe = Endpoint(routePath ?? Doc.p("Subscribe to notifications for $name"))
    .in[ServiceEndpoint]
    .out[Subject.SubscriptionId]
    .decorate

  val unsubscribe =
    Endpoint(Method.DELETE / self.version / self.name / "unsubscribe")
      .in[Subject.SubscriptionId]
      // .query(HttpCodec.queryAll[Subject.SubscriptionId])
      .out[Boolean]
      .decorate

  val subscriptionOpenAPI = OpenAPIGen.fromEndpoints(
    s"Entity $name Subscription Endpoint",
    version = self.version,
    subscribe,
    unsubscribe
  )

end Subject // class
