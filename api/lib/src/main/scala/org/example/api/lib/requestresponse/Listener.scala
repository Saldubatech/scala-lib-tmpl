package org.example.api.lib.requestresponse

import org.example.api.lib.bindings.ServiceEndpoint
import zio.http.endpoint.Endpoint
import zio.http.Method
import zio.http.codec.*
import zio.schema.{DeriveSchema, Schema}

object Listener:

end Listener

class Listener[N <: Notification](location: ServiceEndpoint)(using nSchema: Schema[N]):

  private given HttpContentCodec[Notification.Ack] = HttpContentCodec.fromSchema(Notification.Ack.schema)

  val accept = Endpoint((Method.POST / location.name) ?? Doc.p("Accept a notification"))
    .in[N]
    .out[Notification.Ack]

end Listener // class
