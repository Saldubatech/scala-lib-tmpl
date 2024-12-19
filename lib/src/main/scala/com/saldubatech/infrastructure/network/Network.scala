package com.saldubatech.infrastructure.network

import com.saldubatech.infrastructure.container.Configuration
import com.saldubatech.lang.types.meta.MetaType
import com.saldubatech.infrastructure.services.ServiceLocalAddress
import zio.json.JsonEncoder
import zio.{ZIO, ZLayer}

object Network:

  enum Protocol derives MetaType, JsonEncoder:

    case local
    case oas3
    case oas3s
    case grpc
    case grpcs

  type Address = String
  type Port    = Int

  case class Host(protocol: Protocol, address: String, port: Int) derives MetaType, JsonEncoder

  sealed trait Endpoint derives MetaType, JsonEncoder:
  end Endpoint

  object Endpoint:
    // case class Local(override val serviceId: String) extends Endpoint derives MetaType

    sealed trait Remote extends Endpoint derives MetaType: // , JsonEncoder:
      val host: Host
    end Remote // trait

    case class Grpc(override val host: Host, service: String) extends Remote derives MetaType // , JsonEncoder

    case class OAS3(override val host: Host, path: List[String] = List.empty) extends Remote derives MetaType // , JsonEncoder

  end Endpoint // object

  case class ServiceLocator(at: Endpoint, address: ServiceLocalAddress) derives MetaType, JsonEncoder
  object ServiceLocator:

    def layer(serviceName: String, serviceVersion: String) =
      ZLayer(
        for {
          apiConfig <- ZIO.service[Configuration.ApiConfig]
        } yield ServiceLocator(
          Network.Endpoint.OAS3(
            host = Network.Host(Network.Protocol.oas3, apiConfig.host, apiConfig.port),
            path = List()
          ),
          ServiceLocalAddress(serviceName, serviceVersion)
        )
      )

  end ServiceLocator // object
end Network          // object
