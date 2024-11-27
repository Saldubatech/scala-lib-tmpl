package org.example.api.tenant

import org.example.api.lib.bindings.{Home, ServiceEndpoint}
import org.example.api.lib.bindings.Home.Protocol
import org.example.api.tenant
import org.example.api.tenant.domain.Tenant
import zio.*
import zio.http.*
import zio.schema.{DeriveSchema, Schema}
import zio.schema.codec.JsonCodec.*
import zio.test.*
import zio.test.TestAspect.*

object ApiComponentSpec extends ZIOSpecDefault:

  given tenantSchema: Schema[Tenant] = DeriveSchema.gen[Tenant]

  def spec =
    suite("tenant CRUD operations")(
      test("get should return a tenant") {
        val req = Request(
          Version.`HTTP/1.1`,
          Method.GET,
          URL(Path.root / "1.0.0-SNAPSHOT" / "tenant" / "333"),
          Headers("X-Request-Id" -> "333")
        )
        for {
          ep        <- ZIO.service[ServiceEndpoint]
          underTest <- ZIO.service[tenant.ApiComponent]
          rs        <- underTest.routes.runZIO(req)
          rsB       <- rs.body.to[Tenant]
        } yield
          val expected = Response(Status.Ok, Headers("X-Request-Id" -> "333"), Body.from(TenantMock.sampleTenant(ep, "do One")))
          assertTrue(rs.status == Status.Ok)
          && assertTrue(rs.headers.get("X-Request-Id") == Some("333"))
          && assertTrue(rsB.id.home == ep)
          && assertTrue(rsB.name == "do One")
      }
    ).provideShared(
      ZLayer.succeed(Home("testHome", Protocol.OAS3, "localhost", 80, None)),
      ServiceEndpoint.layer("tenant", "1.0.0-SNAPSHOT"),
      TenantMock.crudLayer,
      TenantMock.adaptorLayer,
      tenant.ApiComponent.Factory.layer
    ) @@ sequential

end ApiComponentSpec
