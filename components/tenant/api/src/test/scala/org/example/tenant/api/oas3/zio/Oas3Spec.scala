package org.example.tenant.api.oas3.zio

import com.saldubatech.infrastructure.network.oas3.entity.EntityResult
import com.saldubatech.infrastructure.network.Network
import com.saldubatech.infrastructure.network.Network.ServiceLocator
import com.saldubatech.infrastructure.services.ServiceLocalAddress
import org.example.tenant.api
import org.example.tenant.domain.Tenant
import zio.*
import zio.http.*
import zio.schema.{DeriveSchema, Schema}
import zio.schema.codec.JsonCodec.*
import zio.test.*
import zio.test.TestAspect.*

object Oas3Spec extends ZIOSpecDefault:

  import com.saldubatech.lang.types.meta.MetaType.given

  def spec =
    suite("tenant CRUD operations")(
      test("get should return a tenant") {
        val req = Request(
          Version.`HTTP/1.1`,
          Method.GET,
          URL(Path.root / "1.0.0-SNAPSHOT" / "tenant" / "LOCAL_ID"),
          Headers("X-Request-Id" -> "333")
        )
        for {
          ep <-
            ZIO.service[ServiceLocator]
          underTest <-
            ZIO.service[TenantOas3Component.Routes]
          rs <-
            underTest.routes.runZIO(req)
          rsB <-
            rs.body.to[EntityResult[Tenant]]
        } yield
          val expected =
            Response(Status.Ok, Headers("X-Request-Id" -> "333"), Body.from(TenantMock.sampleTenantEntity(ep, "do One")))
          assertTrue(rs.status == Status.Ok)
          && assertTrue(rs.headers.get("X-Request-Id") == Some("333"))
          && assertTrue(rsB.id.home == ep)
          && assertTrue(rsB.payload.name == "do One")
      }
    ).provideShared(
      ZLayer.succeed(
        ServiceLocator(
          Network.Endpoint.OAS3(Network.Host(Network.Protocol.oas3, "localhost", 80)),
          ServiceLocalAddress("tenant", "1.0.0-SNAPSHOT")
        )
      ),
      TenantOas3Component.routesLayer,
      TenantOas3Component.endpointLayer,
      TenantOas3Component.adaptorLayer,
      TenantMock.crudLayer
//      TenantMock.adaptorLayer
    ) @@ sequential

end Oas3Spec
