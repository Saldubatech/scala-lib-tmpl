package com.saldubatech.infrastructure.storage.rdbms.datasource

import zio.{URLayer, ZIO, ZLayer}
import com.typesafe.config
import io.getquill.JdbcContextConfig
import io.getquill.jdbczio.Quill

import javax.sql.DataSource

object DataSource:
  val layer: URLayer[DataSourceBuilder, DataSource] = ZLayer(ZIO.serviceWith[DataSourceBuilder](_.dataSource))
end DataSource // object

trait DataSourceBuilder:
  def dataSource: DataSource

object DataSourceBuilder:

  abstract class SimpleDbConfiguration(val user: String, val pwd: String, val dbName: String, val server: String, val port: Int):
    lazy val connectionString: String
  end SimpleDbConfiguration // class

  def layer(dbConfigLayer: URLayer[config.Config, JdbcContextConfig]) =
    for {
      env <- dbConfigLayer
      ds  <- Quill.DataSource.fromJdbcConfig(env.get)
    } yield ds

end DataSourceBuilder // object
