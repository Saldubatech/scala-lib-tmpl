package org.example.api.lib

import com.saldubatech.lang.types.AppResult
import org.example.api.lib.bindings.ServiceEndpoint
import org.example.api.lib.requestresponse.{EntityOperations, Subject}
import zio.{Tag as ZTag, ZIO, ZLayer}

object ApiComponent:

  trait Operations[E: ZTag, S: ZTag]:

    type ServiceCrud
    type ServiceAdaptor = EntityOperations.Adaptor[E, S] & Subject.Adaptor
    type Endpoint       = EntityOperations[E, S] with Subject
    type Implementation = EntityOperations.Implementation[E, S, ServiceAdaptor]

    val serviceAdaptorLayer: ZLayer[ServiceEndpoint & ServiceCrud, AppResult.Error, ServiceAdaptor]
    def implementation(ep: ServiceEndpoint): Implementation

    val routesLayer = ZLayer(
      for {
        implementation <- ZIO.service[Implementation]
      } yield implementation.routes ++ implementation.swaggerRoutes
    )

  end Operations

  trait Factory[E, S, Adaptor <: EntityOperations.Adaptor[E, S]]:
    def layer: ZLayer[Adaptor & ServiceEndpoint, AppResult.Error, ApiComponent[E, S, Adaptor]]
  end Factory

end ApiComponent

trait ApiComponent[E, S, Adaptor <: EntityOperations.Adaptor[E, S]]:

  type Endpoint <: EntityOperations[E, S] with Subject
  type Implementation <: EntityOperations.Implementation[E, S, Adaptor]

  val endpoint: Endpoint
  val implementation: Implementation

end ApiComponent
