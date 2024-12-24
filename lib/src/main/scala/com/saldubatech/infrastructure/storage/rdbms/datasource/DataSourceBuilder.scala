package com.saldubatech.infrastructure.storage.rdbms.datasource

import zio.{URLayer, ZIO, ZLayer}
import com.typesafe.config
import io.getquill.JdbcContextConfig
import io.getquill.jdbczio.Quill

import javax.sql.DataSource

object DataSource:

  val layer =
    ZLayer
      .fromFunction((dsC: JdbcContextConfig) => Quill.DataSource.fromJdbcConfig(dsC))
      .flatten

end DataSource // object

trait DataSourceBuilder:
  def dataSource: DataSource

object DataSourceBuilder:

  abstract class SimpleDbConfiguration(val user: String, val pwd: String, val dbName: String, val server: String, val port: Int):
    lazy val connectionString: String
  end SimpleDbConfiguration // class

end DataSourceBuilder // object
