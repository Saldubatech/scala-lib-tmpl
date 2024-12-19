package com.saldubatech.infrastructure.network.oas3.entity

import com.saldubatech.infrastructure.network.Network.ServiceLocator
import com.saldubatech.lang.Id as LId
import com.saldubatech.lang.types.meta.MetaType
import zio.schema.{DeriveSchema, Schema}

object EntityResult:

  case class Id(home: ServiceLocator, localId: LId) derives MetaType

  trait API[P]:

    def entity(id: Id, payload: P): EntityResult[P] = EntityResult(id, payload)
    def payload(e: EntityResult[P]): P              = e.payload

  end API // trait

  implicit def api[P]: API[P] = new API[P] {}

end EntityResult

case class EntityResult[P](id: EntityResult.Id, payload: P) derives MetaType:
  def map[T: MetaType: Schema](f: P => T): EntityResult[T] = EntityResult(id, f(payload))

end EntityResult // trait
