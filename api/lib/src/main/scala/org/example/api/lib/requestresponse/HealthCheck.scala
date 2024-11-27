package org.example.api.lib.requestresponse

import zio.*
import zio.http.*

object HealthCheck:

  enum HealthStatus:

    case OK
    case NOK

  trait Service:
    def check: UIO[HealthStatus]

  val service: URIO[Service, HealthStatus] = ZIO.serviceWithZIO[Service](_.check)
  
  val dummyLayer = ZLayer.succeed( new Service {
    override def check: UIO[HealthStatus] = ZIO.succeed(HealthStatus.OK)
  })

  val routes = Routes(
    Method.HEAD / "healthcheck" -> handler { (req: Request) =>
      ZIO.succeed(Response.status(Status.NoContent))
    },
    Method.GET / "healthcheck" -> handler { (req: Request) =>
      service.map {
        case HealthStatus.OK  => Response(Status.Ok, Headers.empty, Body.fromString("A_OK"))
        case HealthStatus.NOK => Response.status(Status.InternalServerError)
      }
    }
  )
