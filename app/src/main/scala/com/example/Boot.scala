package com.example

import com.saldubatech.healthcheck.{HealthCheckComponent, HealthCheckRoutes, HealthCheckService}
import org.example.tenant.TenantComponent
import com.saldubatech.infrastructure.container.{App, Configuration}
import com.saldubatech.infrastructure.network.Network.ServiceLocator
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

  private val fwConfigLayer       = DbMigration.FlywayConfiguration.layer("flyway")
  private val apiConfigLayer      = Configuration.ApiConfig.layer("api")
  private val databaseConfigLayer = Configuration.DbConfig.layer("db")

  val tenantServiceName: String    = "tenant"
  val tenantServiceVersion: String = "1.0.0-SNAPSHOT"

  private val tenantServiceLocatorLayer = ServiceLocator.layer(tenantServiceName, tenantServiceVersion)

  private val bootEffect =
    ZIO
      .service[TenantComponent.Routing]
      .zipPar(ZIO.service[HealthCheckRoutes])
      .map(p => p._1.routes ++ p._2.routes)
      .zipPar(DbMigration.migrationEffect)
      .map(_._1)

  private val runner =
    for {
      routes  <- bootEffect
      serving <- Server.serve(routes)
    } yield serving

  override val run =
    runner.provide(bootstrap, App.serverLayer, rootConfigLayer, databaseConfigLayer, fwConfigLayer, DataSource.layer,
      DbMigration.flywayLayer, Quill.Postgres.fromNamingStrategy(Literal), HealthCheckComponent.layer, TenantJournal.layer,
      apiConfigLayer, tenantServiceLocatorLayer, TenantService.defaultLayer, TenantComponent.layer)
