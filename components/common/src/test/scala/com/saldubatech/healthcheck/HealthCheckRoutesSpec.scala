package com.saldubatech.healthcheck

import zio.http.*
import zio.test.*
import zio.test.Assertion.*
import zio.ZIO

object HealthCheckRoutesSpec extends ZIOSpecDefault:

  val specs: Spec[HealthCheckService.Interface & HealthCheckRoutes, Nothing] =
    suite("health check")(
      test("ok status") {
        for {
          actual <- ZIO.service[HealthCheckRoutes]
          rs     <- actual.routes.runZIO(Request.get(URL(Path.root / "healthcheck")))
        } yield assert(rs)(equalTo(Response(Status.Ok, Headers.empty, Body.fromString("A_OK"))))
      }
    )

  override def spec: Spec[Any, Nothing] =
    specs.provide(
      HealthCheckServiceTest.layer,
      HealthCheckComponent.routesLayer
    )
