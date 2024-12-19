package com.saldubatech.infrastructure.storage.rdbms.quill

import com.saldubatech.lang.query.{Projectable, Projection}
import com.saldubatech.lang.types.*

object DynamicProjection:

  private[quill] def project(p: Projection): AppResult[String] =
    val groups = p.path.groupMap {
      case idx: Projectable.Index   => "index"
      case field: Projectable.Field => "field"
    } {
      case _: Projectable.Index => "index"
      case f: Projectable.Field => f.name
    }
    (groups.get("index"), groups.get("field")) match
      case (Some(idx), _) if idx.nonEmpty       => AppResult.fail(s"Index Paths are not supported for querying the database: $idx")
      case (_, Some(fields)) if fields.nonEmpty => AppResult.Success(fields.last) // Only the last element to match Quills naming of columns.
      case (_, _)                               => AppResult.fail(s"No valid path to retrieve in projection: $p")

end DynamicProjection
