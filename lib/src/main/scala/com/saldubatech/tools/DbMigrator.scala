package com.saldubatech.tools

import com.saldubatech.infrastructure.storage.rdbms.datasource.PGDataSourceBuilder
import com.saldubatech.infrastructure.storage.rdbms.migration.DbMigration
import com.typesafe.config.{Config, ConfigFactory}
import org.flywaydb.core.api.output.MigrateResult
import zio.{IO, RIO, Task, ZIO, ZIOAppDefault}

object DbMigrator extends ZIOAppDefault:
//  override val bootstrap =
//    FlywayMigrations.migrationLayer
//    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private val config: Config     = ConfigFactory.defaultApplication().resolve()
  private val dbTSConfig: Config = config.getConfig("db")
  private val pgConfig           = PGDataSourceBuilder.Configuration(dbTSConfig)
  private val fwyTSConfig        = config.getConfig("flyway")
  private val flywayConfig       = DbMigration.FlywayConfiguration(fwyTSConfig)

  override val run: Task[Int] = DbMigration.migrator.provide(
    DbMigration.standaloneFlywayLayer(flywayConfig, pgConfig)
  )
