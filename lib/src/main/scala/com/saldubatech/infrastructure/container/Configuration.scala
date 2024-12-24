package com.saldubatech.infrastructure.container

import com.typesafe.config
import com.typesafe.config.ConfigFactory
import io.getquill.JdbcContextConfig
import zio.{Config, ULayer, ZIO, ZLayer}
import zio.config.*
import zio.config.typesafe.*

import scala.jdk.CollectionConverters.*

object Configuration:

  val defaultRootConfigLayer: ULayer[config.Config] = ZLayer.succeed(ConfigFactory.defaultApplication().resolve())

  // Look at this Configuration Documentation for extensions: https://zio.dev/zio-http/reference/server#starting-a-server-with-custom-configurations
  // And how to handle it through ZIO.config: https://zio.dev/zio-http/reference/server#integration-with-zio-config
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

  object DbConfig:

    def layer(path: String) =
      ZLayer(
        for {
          rootConfig <- ZIO.service[config.Config]
        } yield JdbcContextConfig(rootConfig.getConfig(path).resolve())
      )

  end DbConfig

end Configuration
