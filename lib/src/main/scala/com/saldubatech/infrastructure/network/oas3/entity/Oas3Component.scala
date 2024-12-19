package com.saldubatech.infrastructure.network.oas3.entity

import com.saldubatech.infrastructure.container.Configuration
import com.saldubatech.infrastructure.network.Network
import com.saldubatech.infrastructure.network.Network.ServiceLocator
import com.saldubatech.infrastructure.services.{Entity, EntityService}
import zio.{RLayer, Tag as ZTag, URLayer, ZIO, ZLayer}

class Oas3Component[
    E <: Entity: ZTag,
    S: ZTag,
    ES <: EntityService[E]: ZTag,
    EP <: EntityEndpoint[E, S]: ZTag,
    ADAPT <: Adaptor[E, S]: ZTag,
    RT <: Routes[E, S, ADAPT]: ZTag
  ](epBuilder: (String, String) => EP,
    adaptBuilder: (ES, Network.Endpoint) => ADAPT,
    routesBuilder: EP => RT):

  val endpointLayer = ZLayer.fromFunction((l: ServiceLocator) => epBuilder(l.address.name, l.address.version))

  val adaptorLayer = ZLayer.fromFunction((service: ES, svcLoc: ServiceLocator) => adaptBuilder(service, svcLoc.at))

  val routesLayer = ZLayer.fromFunction(routesBuilder)

  val layer: RLayer[ServiceLocator & ES, ADAPT & RT] = endpointLayer >>> (adaptorLayer ++ routesLayer)

end Oas3Component
