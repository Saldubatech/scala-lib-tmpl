package org.example.api.lib

import org.example.api.lib.requestresponse.standardErrors
import zio.http.codec.HeaderCodec
import zio.http.endpoint.{AuthType, Endpoint}

package object requestresponse:

  val requestIdCodec = HeaderCodec.name[Long]("X-Request-Id")

  extension [PathInput, Input, Output, Auth <: AuthType](
      ep: zio.http.endpoint.Endpoint[PathInput, Input, zio.ZNothing, Output, Auth]
    )

    // Endpoint[PathInput, (Input, Long), Error, (Output, Long), Auth]
    inline def decorate =
      ep
        .header(requestIdCodec)
        .outHeader(requestIdCodec)
        .standardErrors

end requestresponse // packageobject
