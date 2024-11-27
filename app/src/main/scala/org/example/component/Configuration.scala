package org.example.component

import com.typesafe.config.ConfigFactory
import com.typesafe.config
import zio.config.*
import zio.{Config, ConfigProvider, ULayer, ZIO, ZLayer}
//import zio.config.ConfigDescriptor._
//import zio.config.typesafe.TypesafeConfigSource
import zio.config.typesafe.*
//import magnolia._

import scala.jdk.CollectionConverters.*

object Configuration:

  val defaultRootConfigLayer: ULayer[config.Config] = ZLayer.succeed(ConfigFactory.defaultApplication().resolve())

  case class ApiConfig(host: String, port: Int)

  object ApiConfig:

    val singleApiConfig: Config[ApiConfig] = apiConfig("api")

    def apiConfig(path: String): Config[ApiConfig] =
      Config.Nested(path, Config.string("host") ++ Config.int("port")).to[ApiConfig]

    def layer(path: String): ZLayer[config.Config, Config.Error, ApiConfig] =
      ZLayer(
        for {
          rootConfig <- ZIO.service[config.Config]
          rs <- read(
                  apiConfig(path).from(
                    TypesafeConfigProvider.fromTypesafeConfig(
                      ConfigFactory.defaultApplication().resolve()
                    )
                  )
                )
        } yield rs
      )

  end ApiConfig // object

  trait DbConfig:

    val serverName: String
    val portNumber: Int
    val user: String
    val password: String
    val databaseName: String

  case class JdbcContextConfig(
      override val serverName: String,
      override val portNumber: Int,
      override val user: String,
      override val password: String,
      override val databaseName: String)
      extends DbConfig:

    val configuration =
      ConfigFactory.parseMap(
        Map[String, Any](
          "serverName"   -> serverName,
          "portNumber"   -> portNumber,
          "user"         -> user,
          "password"     -> password,
          "databaseName" -> databaseName
        ).asJava
      )

  object DbConfig:

    def jdbcContextConfig(path: String) =
      Config
        .Nested(
          path,
          Config.string("serverName") ++
            Config.int("portNumber") ++
            Config.string("user") ++
            Config.string("password") ++
            Config.string("databaseName")
        )
        .to[JdbcContextConfig]

    val singleJdbcContextConfig = jdbcContextConfig("db")

    def layer(path: String): ZLayer[config.Config, Config.Error, JdbcContextConfig] =
      ZLayer(
        for {
          rootConfig <- ZIO.service[config.Config]
          rs <- read(
                  jdbcContextConfig(path).from(
                    TypesafeConfigProvider.fromTypesafeConfig(
                      ConfigFactory.defaultApplication().resolve()
                    )
                  )
                )
        } yield rs
      )

end Configuration
