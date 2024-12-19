package com.saldubatech.infrastructure.storage.rdbms.quill

import com.saldubatech.infrastructure.storage.{
  DataRecord,
  Domain as D,
  InsertionError,
  NotFoundError,
  PersistenceError,
  RepositoryError,
  Term,
  TooManyResultsError,
  ValidationError
}
import com.saldubatech.lang.Id
import com.saldubatech.lang.types.DIO
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.{IO, ZIO}

import java.sql.SQLException

object Domain:

  trait Service[R <: DataRecord] extends D.Service[R]:

    def dynamicFind(d: DynamicPredicate[R]): DIO[Iterable[R]]
    def dynamicCount(d: DynamicPredicate[R]): DIO[Long]

  end Service

  trait ServiceInliner[R <: DataRecord](val quillCtx: Quill.Postgres[Literal]) extends D.Service[R]:

    import quillCtx.*

    // Utility to help implement the adder method
    protected inline def adder(inline baseQuery: Quoted[EntityQuery[R]]): R => DIO[R] =
      (r: R) =>
        transaction(
          for {
            i1 <- run(baseQuery.filter(_.rId == lift(r.rId)).map(_.rId).size)
            i2 <- if i1 == 0 then run(baseQuery.insertValue(lift(r)).returning(rr => rr))
                  else ZIO.fail(InsertionError(s"Cannot Insert record with duplicate Id: ${r.rId}"))
          } yield i2
        ).handleExceptions

    // Get by Id utility
    protected inline def getter(inline baseQuery: Quoted[EntityQuery[R]]): Id => DIO[R] =
      rId =>
        run(quote(baseQuery.filter(_.rId == lift(rId)))).handleExceptions.flatMap {
          case Nil      => ZIO.fail(NotFoundError(rId))
          case f :: Nil => ZIO.succeed(f)
          case other    => ZIO.fail(TooManyResultsError(rId))
        }

    // Utility to help implement the finder method
    protected inline def allFinder(inline baseQuery: Quoted[EntityQuery[R]]): DIO[Iterable[R]] =
      run(quote(baseQuery)).handleExceptions

    // Utility to help implement the counter method
    protected inline def allCounter(inline baseQuery: Quoted[EntityQuery[R]]): DIO[Long] =
      run(quote(baseQuery.size)).handleExceptions

    // Utility to help implement the deleter function
    protected inline def remover(inline baseQuery: Quoted[EntityQuery[R]]): Id => DIO[R] =
      (rId: Id) =>
        transaction(
          for {
            i1 <- run(baseQuery.filter(_.rId == lift(rId)).map(_.rId).size)
            i2 <- if i1 == 1 then run(baseQuery.filter(_.rId == lift(rId)).delete.returning(r => r))
                  else if i1 > 1 then ZIO.fail(ValidationError(s"Too many records to delete: $i1"))
                  else ZIO.fail(NotFoundError(rId))
          } yield i2
        ).handleExceptions

    protected inline def updater(inline baseQuery: Quoted[EntityQuery[R]]): R => DIO[R] =
      (r: R) =>
        transaction(
          for {
            i1 <- run(baseQuery.filter(_.rId == lift(r.rId)).map(_.rId).size)
            i2 <- if i1 == 1 then run(baseQuery.filter(_.rId == lift(r.rId)).updateValue(lift(r)).returning(r => r))
                  else if i1 > 1 then ZIO.fail(ValidationError(s"Too many records to update $i1"))
                  else ZIO.fail(NotFoundError(r.rId))
          } yield i2
        ).handleExceptions

    protected inline def attributeFinder[V: Encoder](inline bq: EntityQuery[R], inline selector: R => V): V => DIO[Iterable[R]] =
      (p: V) => run(quote(bq.filter(host => selector(host) == lift(p)))).handleExceptions

    protected inline def finder(inline bq: EntityQuery[R], inline selector: R => Boolean): DIO[Iterable[R]] =
      run(quote(bq.filter(r => selector(r)))).handleExceptions

    protected inline def dynamicFinder(inline bq: EntityQuery[R]): DynamicPredicate[R] => DIO[Iterable[R]] =
      (d: DynamicPredicate[R]) =>
        import Domain.*
        for {
          tr <- quillCtx.translate(d(bq)).handleExceptions
          rs <- run(d(bq)).handleExceptions
        } yield rs

    protected inline def counter(inline baseQuery: Quoted[EntityQuery[R]], inline selector: R => Boolean): DIO[Long] =
      run(quote(baseQuery.filter(r => selector(r)).size)).handleExceptions

    protected inline def dynamicCounter(inline bq: EntityQuery[R]): DynamicPredicate[R] => DIO[Long] =
      (d: DynamicPredicate[R]) =>
        import Domain.*
        for {
          tr <- quillCtx.translate(d(bq)).handleExceptions
          rs <- run(d.count(bq)).handleExceptions
        } yield rs

  end ServiceInliner // trait

  extension [RS](o: IO[Throwable, RS])

    def handleExceptions: DIO[RS] =
      o.refineOrDie {
        case e: SQLException      => RepositoryError.fromThrowable(e)
        case pe: PersistenceError => pe
      }

class Domain
