package com.saldubatech.healthcheck

import io.getquill.jdbczio.Quill
import io.getquill.Literal
import zio.{URLayer, ZIO, ZLayer}

object HealthCheckComponent:

  val routesLayer: URLayer[HealthCheckService.Interface, HealthCheckRoutes] =
    ZLayer(
      for {
        svc <- ZIO.service[HealthCheckService.Interface]
      } yield HealthCheckRoutes(svc)
    )

  val serviceLayer: URLayer[Quill.Postgres[Literal], HealthCheckService.Interface] =
    ZLayer {
      for {
        quill <- ZIO.service[Quill.Postgres[Literal]]
      } yield HealthCheckService(quill)
    }

  val layer = serviceLayer >+> routesLayer

end HealthCheckComponent
