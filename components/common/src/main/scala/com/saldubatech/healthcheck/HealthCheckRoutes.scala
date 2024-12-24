package com.saldubatech.healthcheck

import zio.*
import zio.http.*

class HealthCheckRoutes(svc: HealthCheckService.Interface):

  val routes: Routes[HealthCheckService.Interface, Nothing] = Routes(
    Method.HEAD / "healthcheck" -> handler { (req: Request) =>
      ZIO.succeed(Response.status(Status.NoContent))
    },
    Method.GET / "healthcheck" -> handler { (req: Request) =>
      svc.check.map { dbStatus =>
        if dbStatus.status then Response(Status.Ok, Headers.empty, Body.fromString("A_OK"))
        else Response.status(Status.InternalServerError)
      }
    }
  )

end HealthCheckRoutes
