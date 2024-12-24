package com.saldubatech.infrastructure.network.oas3.entity

import com.saldubatech.infrastructure.network.Network
import com.saldubatech.infrastructure.network.Network.ServiceLocator
import com.saldubatech.infrastructure.services.{Entity, EntityService}
import com.saldubatech.lang.types.meta.MetaType
import zio.{URLayer, ZIO, ZLayer, Tag as ZTag}

trait Oas3Component[E <: Entity: ZTag: MetaType, SUMMARY: ZTag: MetaType](
    using summarizer: Option[Adaptor.Summarizer[E, SUMMARY]] = None):

  type ES <: EntityService[E]

  class Endpoint(override val name: String, override val version: String)              extends EntityEndpoint[E, SUMMARY]
  class ServiceAdaptor(override val service: ES, override val forEndpoint: Network.Endpoint) extends Adaptor[E, SUMMARY]

  final type Routing = Routes[E, SUMMARY, ServiceAdaptor]

  final inline def endpointLayer: URLayer[ServiceLocator, Endpoint] =
    ZLayer(
      for {
        svcLoc <- ZIO.service[ServiceLocator]
      } yield Endpoint(svcLoc.address.name, svcLoc.address.version)
    )

  final inline def routesLayer: URLayer[Endpoint, Routing] =
    ZLayer(ZIO.service[Endpoint].map(Routes.apply[E, SUMMARY, ServiceAdaptor](_)))

  final inline def adaptorLayer: URLayer[ServiceLocator & ES, ServiceAdaptor] =
    ZLayer(
      for {
        svc    <- ZIO.service[ES]
        svcLoc <- ZIO.service[ServiceLocator]
      } yield ServiceAdaptor(svc, svcLoc.at)
    )

  final inline def layer: URLayer[ServiceLocator & ES, ServiceAdaptor & Routing] = endpointLayer >>> (adaptorLayer ++ routesLayer)

end Oas3Component
