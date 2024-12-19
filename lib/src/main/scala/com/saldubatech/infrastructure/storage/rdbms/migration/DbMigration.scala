package com.saldubatech.infrastructure.storage.rdbms.migration

import com.saldubatech.infrastructure.storage.rdbms.datasource.{DataSourceBuilder, PGDataSourceBuilder}
import com.saldubatech.lang.types.*
import com.saldubatech.util.LogEnabled
import com.typesafe.config.Config
import org.flywaydb.core.api.{FlywayException, Location}
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.{MigrateOutput, MigrateResult}
import zio.{RIO, Task, TaskLayer, ULayer, URIO, URLayer, ZIO, ZLayer}

import javax.sql.DataSource
import scala.jdk.CollectionConverters.*
import scala.util.Try

object DbMigration:

  val migrationEffect: ZIO[DbMigration, Throwable, MigrateResult] = ZIO.serviceWithZIO(srv => srv.doMigrate())

  val migrator: RIO[DbMigration, Int] = migrationEffect.flatMap { mr =>
    val errs: Iterable[Throwable] = mr.getFailedMigrations.asScala.map(mOut => AppResult.Error(mOut.description.toString))
    if errs.sizeIs == 0 then ZIO.succeed(mr.migrationsExecuted)
    else ZIO.fail(AppResult.Error("Not All Migrations Succeeded")) // , causes = errs))
  }

  case class FlywayConfiguration(locations: List[String], adminTable: String)

  object FlywayConfiguration:

    def apply(flywayConfig: Config): FlywayConfiguration =
      FlywayConfiguration(
        flywayConfig.getStringList("locations").asScala.toList,
        flywayConfig.getString("migrationTable")
      )

    def layer(path: String) =
      ZLayer(
        for {
          rootConfig <- ZIO.service[Config]
        } yield FlywayConfiguration(rootConfig.getConfig(path).resolve())
      )

  end FlywayConfiguration // object

  def standaloneFlywayLayer(fwConfig: FlywayConfiguration, dbConfig: DataSourceBuilder.SimpleDbConfiguration)
      : TaskLayer[DbMigration] =
    ZLayer.fromZIO(
      ZIO.attempt(StandAloneFlywayMigration(fwConfig, dbConfig))
    )

  val flywayLayer: URLayer[DataSource & FlywayConfiguration, FlywayMigration2] =
    ZLayer(
      for {
        ds  <- ZIO.service[DataSource]
        cfg <- ZIO.service[FlywayConfiguration]
      } yield FlywayMigration2(ds, cfg)
    )

end DbMigration

trait DbMigration extends LogEnabled:
  def doMigrate(): Task[MigrateResult]
end DbMigration // trait

class FlywayMigration2(ds: DataSource, config: DbMigration.FlywayConfiguration) extends DbMigration:

  private def logValidationErrorsIfAny(fwy: Flyway): Unit = {
    val validated = fwy.validateWithResult()

    if !validated.validationSuccessful then
      for error <- validated.invalidMigrations.asScala
      do log.warn(s"""
                     |Failed validation:
                     |  - version:      ${error.version}
                     |  - path:         ${error.filepath}
                     |  - description:  ${error.description}
                     |  - errorCode:    ${error.errorDetails.errorCode}
                     |  - errorMessage: ${error.errorDetails.errorMessage}

           """.stripMargin.strip)
  }

  override def doMigrate(): Task[MigrateResult] =
    val cfg: FluentConfiguration = Flyway.configure
      .dataSource(ds)
      .group(true)
      .validateMigrationNaming(true)
      .outOfOrder(false)
      .table(config.adminTable)
      .locations(config.locations.map(Location(_))*)
      .baselineOnMigrate(true)
    ZIO.fromTry(Try {
      val migration: Flyway = cfg.load()
      val rs                = migration.migrate()
      if !rs.success then
        rs.getFailedMigrations.asScala.foreach { failed =>
          log.warn(s"""
                      |Failed Migration:
                      |  - Category:       ${failed.category}
                      |  - Version:        ${failed.version}
                      |  - Description:    ${failed.description}
                      |  - Type:           ${failed.`type`}
                      |  - File Path:      ${failed.filepath}
                      |  - Execution Time: ${failed.executionTime}
                      |""".stripMargin)
        }
      logValidationErrorsIfAny(migration)
      rs
    })

class StandAloneFlywayMigration(config: DbMigration.FlywayConfiguration, dbConfig: DataSourceBuilder.SimpleDbConfiguration)
    extends DbMigration:

  override def doMigrate(): Task[MigrateResult] =
    log.info(
      s"DB Migration will be done with: '${dbConfig.connectionString}'" +
        s" for user: '${dbConfig.user}'"
    )
    log.info(s"With Schemas located in ${config.locations}")
    val cfg: FluentConfiguration = Flyway.configure
      .dataSource(
        dbConfig.connectionString,
        dbConfig.user,
        dbConfig.pwd
      )
      .group(true)
      .outOfOrder(false)
      .table(config.adminTable)
      .locations(config.locations.map(Location(_))*)
      .baselineOnMigrate(true)
    logValidationErrorsIfAny(cfg)

    ZIO.fromTry(Try(cfg.load().migrate()))

  private def logValidationErrorsIfAny(cfg: FluentConfiguration): Unit = {
    val validated = cfg.load().validateWithResult()

    if !validated.validationSuccessful then
      for error <- validated.invalidMigrations.asScala
      do log.warn(s"""
                     |Failed validation:
                     |  - version: ${error.version}
                     |  - path: ${error.filepath}
                     |  - description: ${error.description}
                     |  - errorCode: ${error.errorDetails.errorCode}
                     |  - errorMessage: ${error.errorDetails.errorMessage}

           """.stripMargin.strip)
  }
