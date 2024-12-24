package com.saldubatech.healthcheck

import zio.*

final class HealthCheckServiceTest extends HealthCheckService.Interface:

  override def check: UIO[DbStatus] = ZIO.succeed(DbStatus(true))

object HealthCheckServiceTest:

  val layer: ULayer[HealthCheckService.Interface] = ZLayer {
    ZIO.succeed(HealthCheckServiceTest())
  }
