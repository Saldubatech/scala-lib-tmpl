package com.saldubatech.infrastructure.container

import com.saldubatech.util.LogEnabled
import zio.{Runtime, ULayer, ZIO, ZIOAppDefault, ZIOAppVersionSpecific, ZLayer}
import zio.http.Server
import zio.logging.backend.SLF4J
import com.typesafe.config

trait App extends ZIOAppDefault with LogEnabled:

  override val bootstrap: ULayer[Unit] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  protected val rootConfigLayer: ULayer[config.Config] = Configuration.defaultRootConfigLayer

end App // trait

object App:

  val serverLayer =
    ZLayer
      .service[Configuration.ApiConfig]
      .flatMap { cfg =>
        Server.defaultWith(_.binding(cfg.get.host, cfg.get.port))
      }
      .orDie

end App // object
