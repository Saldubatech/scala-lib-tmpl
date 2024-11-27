package org.example.api.lib.types

type UUID = String
object UUID:
  def apply(): UUID = java.util.UUID.randomUUID().toString
end UUID
