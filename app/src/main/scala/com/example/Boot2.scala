package com.example

import com.example.domain.TenantMock
import com.typesafe.config
import org.example.api.lib.bindings.{Home, ServiceEndpoint}
import org.example.api.lib.requestresponse.{APIError, HealthCheck}
import org.example.api.tenant
import org.example.component.Configuration
import zio.*
import zio.http.Server
import zio.json.*
import zio.logging.backend.SLF4J
//import zio.logging.slf4j.bridge.Slf4jBridge

object Boot2 extends ZIOAppDefault:

  given protocolEncoder: JsonEncoder[Home.Protocol]       = DeriveJsonEncoder.gen[Home.Protocol]
  given homeEncoder: JsonEncoder[Home]                    = DeriveJsonEncoder.gen[Home]
  given servicePointEncoder: JsonEncoder[ServiceEndpoint] = DeriveJsonEncoder.gen[ServiceEndpoint]
  given apiErrorEncoder: JsonEncoder[APIError]            = DeriveJsonEncoder.gen[APIError]

//  val home: Home                     = Home.Root(Home.Protocol.OAS3, "localhost", 8080)
//  val root: ServiceEndpoint.Absolute = ServiceEndpoint.Root(home)

  override val bootstrap: ULayer[Unit] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private val configLayer: ULayer[config.Config] = Configuration.defaultRootConfigLayer

  private val apiConfigLayer = Configuration.ApiConfig.layer("api")

  private val dbConfigLayer = Configuration.DbConfig.layer("db")

  private val allHomesConfigLayer = Home.allHomesConfigLayer("homes")

  private val homeConfigLayer = Home.homeConfigLayer("mainOas3")

  private val tenantEndpointLayer = ServiceEndpoint.layer("tenant", "1.0.0-SNAPSHOT")

  private val homeLayer = Home.homeLayer

  // private val dataSourceLayer = Quill.DataSource.fromPrefix("db")

  // private val dbConfig: config.Config = ConfigFactory.defaultApplication().getConfig("db").resolve()

  // private val dataSourceLayer = Quill.DataSource.fromConfig(dbConfig)

//  private val postgresLayer = Quill.Postgres.fromNamingStrategy(Literal)
//
//  private val repoLayer = ItemRepositoryLive.layer

  private val healthCheckServiceLayer = HealthCheck.dummyLayer

  private val serverLayer =
    ZLayer
      .service[Configuration.ApiConfig]
      .flatMap { cfg =>
        Server.defaultWith(_.binding(cfg.get.host, cfg.get.port))
      }
      .orDie

  /*
    : Routes[
    EntityOperations.CrudServiceAdaptor[Tenant, TenantSummary] & HealthCheck.Service & ItemRepository,
    Response
  ]
   */
  // val routes = HealthCheck.routes ++ tenant.Operations.Implementation.routes // ++ Endpoints.routes

  // : URIO[HealthCheckService & ItemRepository & Server & tenant.Operations.ServiceAdaptor, Nothing]
  private val programZIO =
    for {
      ep        <- ZIO.service[ServiceEndpoint]
      component <- ZIO.service[tenant.ApiComponent]
      rs        <- Server.serve(HealthCheck.routes ++ component.routes)
    } yield rs

  override val run =
    programZIO.provide(bootstrap, healthCheckServiceLayer, serverLayer, tenant.ApiComponent.Factory.layer,
      TenantMock.adaptorLayer, TenantMock.crudLayer, tenantEndpointLayer, homeLayer, homeConfigLayer, allHomesConfigLayer,
      apiConfigLayer, configLayer)
