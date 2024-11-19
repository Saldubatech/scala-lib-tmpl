package com.saldubatech.infrastructure.storage

import com.saldubatech.lang.types.*
import com.saldubatech.lang.Id
import zio.IO

sealed class PersistenceError(msg: String, cause: Option[Throwable] = None) extends AppResult.Error(msg, cause)

final case class RepositoryError(override val msg: String, override val cause: Option[Throwable] = None)
    extends PersistenceError(msg, cause)

object RepositoryError:
  def fromThrowable(cause: Throwable): RepositoryError = RepositoryError(cause.getMessage, Some(cause))

case class InsertionError(override val msg: String) extends PersistenceError(msg)

case class ValidationError(override val msg: String) extends PersistenceError(msg)
case class NotFoundError(id: Id)                     extends PersistenceError(s"Error: $id Not Found")
case class TooManyResultsError(id: Id)               extends PersistenceError(s"Error: $id matches too many records")

type PersistenceIO[R] = IO[PersistenceError, R]
