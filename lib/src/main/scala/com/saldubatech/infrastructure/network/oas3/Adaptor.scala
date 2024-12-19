package com.saldubatech.infrastructure.network.oas3

import com.saldubatech.infrastructure.network.Network
import com.saldubatech.infrastructure.network.Network.ServiceLocator
import com.saldubatech.infrastructure.services.Service
import zio.http.codec.HeaderCodec
import zio.http.endpoint.AuthType

trait Adaptor:

  val service: Service
  val forEndpoint: Network.Endpoint
  final val serviceLocator = ServiceLocator(forEndpoint, service.address)

  protected val errorHandler = APIError.Mapper(serviceLocator)

end Adaptor

object Adaptor:

  val requestIdCodec = HeaderCodec.name[Long]("X-Request-Id")

  import APIError.standardErrors

  extension [PathInput, Input, Output, Auth <: AuthType](
      ep: zio.http.endpoint.Endpoint[PathInput, Input, zio.ZNothing, Output, Auth]
    )

    // Endpoint[PathInput, (Input, Long), Error, (Output, Long), Auth]
    inline def decorate =
      ep
        .header(requestIdCodec)
        .outHeader(requestIdCodec)
        .standardErrors

end Adaptor // object
