package com.saldubatech.infrastructure.storage

import com.saldubatech.lang.types.*
import com.saldubatech.lang.Id
import zio.ZIO

import javax.sql.DataSource

object Domain:

  trait Service[R <: DataRecord]:

    def add(r: R): DIO[R]
    def get(rId: Id): DIO[R]
    def findAll: DIO[Iterable[R]]
    inline def find(inline t: Term[R]): DIO[Iterable[R]]
    def countAll: DIO[Long]
    inline def count(inline t: Term[R]): DIO[Long]
    def update(r: R): DIO[R]
    def remove(rId: Id): DIO[R]

  end Service // trait

end Domain // object

trait Domain[R <: DataRecord]:

  def add(r: R): AppResult[R]
  def get(rId: Id): AppResult[R]
  def findAll: AppResult[Iterable[R]]
  def countAll: AppResult[Long]
  def update(r: R): AppResult[R]
  def remove(rId: Id): AppResult[R]

end Domain // trait
