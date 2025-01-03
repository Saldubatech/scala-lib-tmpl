package org.example.api.lib.requestresponse

import com.saldubatech.infrastructure.network.Network.ServiceLocator
import com.saldubatech.lang.types.AppResult
import com.saldubatech.lang.types.datetime.Epoch
import com.saldubatech.lang.Id
import zio.{IO, Tag as ZTag, ZIO, ZNothing}
import zio.http.{handler, Handler, Header, Method, Route, RoutePattern, Routes, Status}
import zio.http.codec.{Doc, HttpCodec}
import zio.http.endpoint.Endpoint
import zio.http.endpoint.openapi.OpenAPIGen
import zio.http.endpoint.AuthType.None
import zio.schema.{DeriveSchema, Schema}

object Subject:

  case class SubscriptionId(home: ServiceLocator, id: Id)
  val subscriptionIdSchema = DeriveSchema.gen[SubscriptionId]

//  trait Adaptor:
//
//    val forEndpoint: ServiceEndpoint
//    val subscribe: ServiceEndpoint => IO[AppResult.Error, Subject.SubscriptionId]
//    val unsubscribe: Subject.SubscriptionId => IO[AppResult.Error, Boolean]
//
//    private val errorHandler = APIError.Mapper(forEndpoint)
//
//    def handleSubscribe(locator: ServiceEndpoint, requestId: Long): IO[APIError, (Subject.SubscriptionId, Long)] =
//      subscribe(locator).mapBoth(errorHandler.map(requestId, Epoch.now, _), subscription => (subscription, requestId))
//
//    def handleUnsubscribe(sId: Subject.SubscriptionId, requestId: Long): IO[APIError, (Boolean, Long)] =
//      unsubscribe(sId).mapBoth(errorHandler.map(requestId, Epoch.now, _), success => (success, requestId))
//
//  end Adaptor

//  trait Implementation[A <: Adaptor: ZTag](endpoint: Subject):
//
//    val subscribeRoute = endpoint.subscribe
//      // handler { (sId: Subject.SubscriptionId, rqId: Long) =>
//      //          ZIO.service[A].flatMap(_.handleUnsubscribe(sId, rqId))
//      .implementHandler(handler { (_: Unit, rqId: Long) =>
//        ZIO.service[A].flatMap(_.handleSubscribe(???, rqId)).map{ _ => "NONE OF THAT"}
//      })
//      .toRoutes
//
//    val unsubscribeRoute =
//      endpoint.unsubscribe
//        // handler { (sId: Subject.SubscriptionId, rqId: Long) =>
//        //          ZIO.service[A].flatMap(_.handleUnsubscribe(sId, rqId))
//        //        }
//        .implementHandler(handler { (_: Unit, rqId: Long) =>
//          ZIO.service[A].flatMap(_.handleUnsubscribe(???, rqId))
//        })
//        .toRoutes
//
//    val subjectRoutes = subscribeRoute ++ unsubscribeRoute
//
//  end Implementation
//
end Subject

//
trait Subject:
//  self: EntityOperations[?, ?] =>
//
//  val routePath: RoutePattern[Unit] = Method.POST / self.version / self.name / "subscribe"
//
//  val subscribe = Endpoint(routePath ?? Doc.p("Subscribe to notifications for $name"))
//    // .in[ServiceEndpoint]
////    .out[Subject.SubscriptionId]
//    .out[String]
//    .decorate
//
//  val unsubscribe =
//    Endpoint(Method.DELETE / self.version / self.name / "unsubscribe")
//      // .in[Subject.SubscriptionId]
//      // .query(HttpCodec.queryAll[Subject.SubscriptionId])
//      .out[Boolean]
//      .decorate
//
//  val subscriptionOpenAPI = OpenAPIGen.fromEndpoints(
//    s"Entity $name Subscription Endpoint",
//    version = self.version,
//    subscribe,
//    unsubscribe
//  )

end Subject // class
