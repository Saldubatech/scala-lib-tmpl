package com.example

import com.example.api.*
import com.example.api.healthcheck.*
import org.example.component.Configuration
import com.example.domain.ItemRepository
import com.example.infrastructure.*
import org.example.api.lib.bindings.{Home, ServiceEndpoint}
import org.example.api.tenant
import com.typesafe.config
import io.getquill.jdbczio.Quill
import io.getquill.Literal
import org.example.api.lib.bindings.Home.Protocol
import org.example.api.lib.requestresponse.{APIError, EntityOperations}
import org.example.api.tenant.domain.{Tenant, TenantSummary}
import zio.*
import zio.config.*
import zio.json.*
import zio.http.{Response, Routes, Server}
import zio.logging.backend.SLF4J
//import zio.logging.slf4j.bridge.Slf4jBridge
import com.typesafe.config.ConfigFactory

object Boot extends ZIOAppDefault:

  given protocolEncoder: JsonEncoder[Protocol]        = DeriveJsonEncoder.gen[Protocol]
  given homeEncoder: JsonEncoder[Home]                = DeriveJsonEncoder.gen[Home]
  given absoluteEncoder: JsonEncoder[ServiceEndpoint] = DeriveJsonEncoder.gen[ServiceEndpoint]
  given apiErrorEncoder: JsonEncoder[APIError]        = DeriveJsonEncoder.gen[APIError]

  override val bootstrap: ULayer[Unit] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  // private val dataSourceLayer = Quill.DataSource.fromPrefix("db")

  private val dbConfig: config.Config = ConfigFactory.defaultApplication().getConfig("db").resolve()

  private val dataSourceLayer = Quill.DataSource.fromConfig(dbConfig)

  private val postgresLayer = Quill.Postgres.fromNamingStrategy(Literal)

  private val repoLayer = ItemRepositoryLive.layer

  private val healthCheckServiceLayer = HealthCheckServiceLive.layer

  private val serverLayer =
    ZLayer
      .service[Configuration.ApiConfig]
      .flatMap { cfg =>
        Server.defaultWith(_.binding(cfg.get.host, cfg.get.port))
      }
      .orDie

//  private val ur: Routes[tenant.Operations.ServiceAdaptor, Response] =
//    val wkw = tenant.Operations.Implementation.routes.mapError { (apiError: APIError) =>
//      Response.json(apiError.toJson).status(apiError.status)
//    } // Response(apiError.status, body = apiError))
//    wkw

  val routes: Routes[
    EntityOperations.Adaptor[Tenant, TenantSummary] & HealthCheckService & ItemRepository,
    Response
  ] =
    HttpRoutes.routes ++ HealthCheckRoutes.routes ++ Endpoints.routes // ++ ur

  // : URIO[HealthCheckService & ItemRepository & Server & tenant.Operations.ServiceAdaptor, Nothing]
  private val program = Server.serve(routes)

  val wkw2 = Configuration.defaultRootConfigLayer >>> Configuration.ApiConfig.layer("db")

  override val run = ???

//    program
//      .provide(
//        bootstrap, healthCheckServiceLayer, serverLayer, Configuration.ApiConfig.layer("db"), repoLayer, postgresLayer,
//        dataSourceLayer, tenant.Operations.serviceAdaptorLayer, Configuration.defaultRootConfigLayer
//      )
