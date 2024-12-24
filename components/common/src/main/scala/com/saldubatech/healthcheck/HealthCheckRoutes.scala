package com.saldubatech.healthcheck

import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.path
import zio.http.endpoint.Endpoint
import zio.http.endpoint.openapi.{OpenAPI, OpenAPIGen, SwaggerUI}

class HealthCheckRoutes(svc: HealthCheckService.Interface):

  private given noContentStatusCodec: StatusCodec[Unit] = HttpCodec.status(Status.NoContent)

  val headEndpoint = Endpoint(Method.HEAD / "healthcheck").out[Unit](Status.NoContent)
  val getEndpoint  = Endpoint(Method.GET / "healthcheck").out[String].outError[String](Status.InternalServerError)

  def openAPI: OpenAPI = OpenAPIGen.fromEndpoints(title = "Healthcheck", version = "1.0.0", headEndpoint, getEndpoint)
  val swaggerRoutes    = SwaggerUI.routes("docs" / "openapi" / "1.0.0" / "healthcheck", openAPI)

  private val functionalRoutes = headEndpoint.implementAs(ZIO.unit).toRoutes ++ (getEndpoint
    .implementHandler(
      handler(svc.check.flatMap { dbStatus =>
        if dbStatus.status then ZIO.succeed("A_OK")
        else ZIO.fail("NOK")
      })
    )
    .toRoutes)

  val routes = functionalRoutes ++ swaggerRoutes

  val routes0: Routes[HealthCheckService.Interface, Nothing] = Routes(
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
