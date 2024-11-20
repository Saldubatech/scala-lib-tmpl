package com.saldubatech.infrastructure.storage.rdbms.quill

import io.getquill.jdbczio.Quill
import io.getquill.SnakeCase
import zio.RLayer

import javax.sql.DataSource

object QuillPostgres:
  val layer: RLayer[DataSource, Quill.Postgres[SnakeCase]] = Quill.Postgres.fromNamingStrategy(SnakeCase)
