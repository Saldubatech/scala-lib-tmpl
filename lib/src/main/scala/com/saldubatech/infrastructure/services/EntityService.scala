package com.saldubatech.infrastructure.services

import com.saldubatech.infrastructure.network.Network.Endpoint
import com.saldubatech.lang.Id
import com.saldubatech.lang.query.Query
import com.saldubatech.lang.types.DIO

trait EntityService[E <: Entity] extends Service:

  def get(eId: Id): DIO[E]
  def query(q: Query): DIO[Iterable[E]]

  def create(e: E): DIO[E]

  def delete(id: Id): DIO[E]

  def update(newValue: E): DIO[E]

end EntityService
