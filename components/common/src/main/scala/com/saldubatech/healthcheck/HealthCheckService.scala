package com.saldubatech.healthcheck

import io.getquill.jdbczio.Quill
import io.getquill.*
import zio.*

object HealthCheckService:

  trait Interface:
    def check: UIO[DbStatus]
  end Interface

end HealthCheckService

class HealthCheckService(quill: Quill.Postgres[Literal]) extends HealthCheckService.Interface:

  import quill.*

  override def check: UIO[DbStatus] =
    run {
      quote {
        sql"""SELECT 1""".as[Query[Int]]
      }
    }
      .fold(
        _ => DbStatus(false),
        _ => DbStatus(true)
      )

end HealthCheckService
