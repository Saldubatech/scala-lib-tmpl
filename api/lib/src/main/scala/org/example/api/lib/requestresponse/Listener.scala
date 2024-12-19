package org.example.api.lib.requestresponse

import com.saldubatech.infrastructure.network.Network.ServiceLocator
import zio.http.endpoint.Endpoint
import zio.http.Method
import zio.http.codec.*
import zio.schema.{DeriveSchema, Schema}

object Listener:

end Listener

class Listener[N <: Notification](location: ServiceLocator)(using nSchema: Schema[N]):

  private given HttpContentCodec[Notification.Ack] = HttpContentCodec.fromSchema(Notification.Ack.schema)

  val accept = Endpoint((Method.POST / location.address.version / location.address.name) ?? Doc.p("Accept a notification"))
    .in[N]
    .out[Notification.Ack]

end Listener // class
