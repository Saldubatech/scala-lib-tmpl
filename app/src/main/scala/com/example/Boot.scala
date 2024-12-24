package com.example

import org.example.tenant.TenantComponent
import com.saldubatech.infrastructure.container.{App, Configuration}
import com.saldubatech.infrastructure.network.Network.ServiceLocator
import com.saldubatech.infrastructure.network.oas3.HealthCheck
import com.saldubatech.infrastructure.network.Network
import com.saldubatech.infrastructure.storage.rdbms.datasource.DataSource
import com.saldubatech.infrastructure.storage.rdbms.migration.DbMigration
import com.typesafe.config
import io.getquill.{JdbcContextConfig, Literal}
import io.getquill.jdbczio.Quill
import org.example.tenant.persistence.TenantJournal
import org.example.tenant.services.TenantService
import zio.*
import zio.http.Server

object Boot extends App:

  private def dbConfigLayer(path: String) =
    ZLayer(
      for {
        rootConfig <- ZIO.service[config.Config]
      } yield JdbcContextConfig(rootConfig.getConfig(path).resolve())
    )

  private val fwConfigLayer       = DbMigration.FlywayConfiguration.layer("flyway")
  private val apiConfigLayer      = Configuration.ApiConfig.layer("api")
  private val databaseConfigLayer = dbConfigLayer("db")

  private val dataSourceLayer = DataSource.layer

  private val postgresLayer = Quill.Postgres.fromNamingStrategy(Literal)

  val serviceName: String    = "tenant"
  val serviceVersion: String = "1.0.0-SNAPSHOT"

  private val serviceLocatorLayer = ServiceLocator.layer(serviceName, serviceVersion)

  private val bootEffect = DbMigration.migrationEffect.zipPar(ZIO.service[TenantComponent.Routing]).map { twoResult =>
    twoResult._2
  }

  // : URIO[HealthCheckService & ItemRepository & Server & tenant.Operations.ServiceAdaptor, Nothing]
  private val runner: ZIO[
    DbMigration & HealthCheck.Service & TenantComponent.ServiceAdaptor & TenantComponent.Routing & zio.http.Server,
    Throwable,
    Nothing
  ] =
    for {
      routes  <- bootEffect
      serving <- Server.serve(HealthCheck.routes ++ routes.routes)
    } yield serving

  override val run =
    runner.provide(bootstrap, HealthCheck.dummyLayer, App.serverLayer, rootConfigLayer, databaseConfigLayer, dataSourceLayer,
      fwConfigLayer, DbMigration.flywayLayer, postgresLayer, TenantJournal.layer, apiConfigLayer, serviceLocatorLayer,
      TenantService.defaultLayer, TenantComponent.layer)
