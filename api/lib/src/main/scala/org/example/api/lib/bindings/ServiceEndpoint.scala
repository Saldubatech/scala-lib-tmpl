package org.example.api.lib.bindings

import zio.{RLayer, ZIO, ZLayer}
import zio.schema.DeriveSchema

case class ServiceEndpoint(home: Home, name: String, version: String)

object ServiceEndpoint:

  val schema = DeriveSchema.gen[ServiceEndpoint]

  def layer(name: String, version: String) = ZLayer(ZIO.service[Home].map(h => ServiceEndpoint(h, name, version)))

end ServiceEndpoint
