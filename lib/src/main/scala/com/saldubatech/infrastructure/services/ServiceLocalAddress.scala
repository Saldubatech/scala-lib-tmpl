package com.saldubatech.infrastructure.services

import com.saldubatech.lang.types.meta.MetaType
import zio.ZLayer
import zio.json.JsonEncoder

case class ServiceLocalAddress(name: String, version: String) derives MetaType, JsonEncoder

object ServiceLocalAddress:
  def layer(name: String, version: String) = ZLayer.succeed(ServiceLocalAddress(name, version))

end ServiceLocalAddress
