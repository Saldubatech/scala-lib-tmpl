package com.saldubatech.infrastructure.storage.rdbms.migration

import com.saldubatech.infrastructure.storage.rdbms.datasource.DataSourceBuilder
import com.saldubatech.lang.types.*
import com.saldubatech.util.LogEnabled
import com.typesafe.config.Config
import org.flywaydb.core.api.{FlywayException, Location}
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.{MigrateOutput, MigrateResult}
import zio.{RIO, Task, TaskLayer, URIO, ZIO, ZLayer}

import scala.jdk.CollectionConverters.*
import scala.util.Try

object DbMigration:

  val service = ZIO.service[DbMigration]

  val migrationEffect: ZIO[DbMigration, Throwable, MigrateResult] = ZIO.serviceWithZIO(srv => srv.doMigrate())

  val migrator: RIO[DbMigration, Int] = migrationEffect.flatMap { mr =>
    val errs: Iterable[Throwable] = mr.getFailedMigrations.asScala.map(mOut => AppResult.Error(mOut.description.toString))
    if errs.sizeIs == 0 then ZIO.succeed(mr.migrationsExecuted)
    else ZIO.fail(AppResult.Error("Not All Migrations Succeeded")) // , causes = errs))
  }

  case class FlywayConfiguration(
      dbConfiguration: DataSourceBuilder.SimpleDbConfiguration,
      locations: List[String],
      adminTable: String)

  object FlywayConfiguration:

    def apply(flywayConfig: Config, dbConfig: DataSourceBuilder.SimpleDbConfiguration): FlywayConfiguration =
      FlywayConfiguration(
        dbConfig,
        flywayConfig.getStringList("locations").asScala.toList,
        flywayConfig.getString("migrationTable")
      )

  def flywayLayer(config: FlywayConfiguration): TaskLayer[DbMigration] =
    ZLayer.fromZIO(
      ZIO.attempt(FlywayMigration(config))
    )

end DbMigration

trait DbMigration extends LogEnabled:
  def doMigrate(): Task[MigrateResult]
end DbMigration // trait

class FlywayMigration(config: DbMigration.FlywayConfiguration) extends DbMigration:

  override def doMigrate(): Task[MigrateResult] =
    log.info(
      s"DB Migration will be done with: '${config.dbConfiguration.connectionString}'" +
        s" for user: '${config.dbConfiguration.user}'"
    )
    log.info(s"With Schemas located in ${config.locations}")
    val cfg: FluentConfiguration = Flyway.configure
      .dataSource(
        config.dbConfiguration.connectionString,
        config.dbConfiguration.user,
        config.dbConfiguration.pwd
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
