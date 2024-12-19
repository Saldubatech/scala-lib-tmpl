package com.saldubatech.lang.query

import com.saldubatech.lang.query.Filter.Literal
import com.saldubatech.lang.types.*
import com.saldubatech.lang.types.meta.MetaType
import zio.json.{JsonDecoder, JsonEncoder}
import zio.optics.{Optic, OpticFailure}
import zio.schema.{DeriveSchema, Schema}
import zio.schema.optics.ZioOpticsBuilder
import zio.schema.TypeId.{Nominal, Structural}

import scala.reflect.Typeable

import ValueType.given

sealed trait Filter derives MetaType, JsonEncoder, JsonDecoder:

end Filter // trait

object Filter:

//  def verify[H <: Product: Projectable](f: Filter): AppResult[Unit] =
//    f match
//      case And(clauses)    => clauses.map(verify).collectAll.unit
//      case Or(clauses)     => clauses.map(verify).collectAll.unit
//      case Not(c)          => verify(c)
//      case cmp: Compare[?] => Compare.verify(cmp)
//      case rg: Range[?]    => Range.verify(rg)
  sealed trait Literal extends Filter derives MetaType
  case object TRUE     extends Literal
  case object FALSE    extends Literal

  sealed trait Composite                extends Filter
  case class And(clauses: List[Filter]) extends Composite derives MetaType
  case class Or(clauses: List[Filter])  extends Composite derives MetaType
  case class Not(filter: Filter)        extends Composite derives MetaType

  object Composite:

  end Composite // object

  sealed trait Compare: // MixIn

    val locator: Projection
    val reference: ValueType.VALUE

  object Compare:

    import ValueType.given

    def unapply(t: Compare): (Projection, ValueType.VALUE) = (t.locator, t.reference)

    import ValueType.given

    case class Eq(override val locator: Projection, override val reference: ValueType.VALUE) extends Filter with Compare
        derives MetaType

    case class Ne(override val locator: Projection, override val reference: ValueType.VALUE) extends Filter with Compare
        derives MetaType

    case class Gt(override val locator: Projection, override val reference: ValueType.VALUE) extends Filter with Compare
        derives MetaType

    case class Ge(override val locator: Projection, override val reference: ValueType.VALUE) extends Filter with Compare
        derives MetaType

    case class Lt(override val locator: Projection, override val reference: ValueType.VALUE) extends Filter with Compare
        derives MetaType

    case class Le(override val locator: Projection, override val reference: ValueType.VALUE) extends Filter with Compare
        derives MetaType

  end Compare

  /* TBD
    Per Gemini:
    ```
      import zio._
      import zio.quill._
      import io.getquill._

      case class User(id: Int, name: String, email: String)

      object UserRepository {
        val ctx = new PostgresContext(Dialect.Postgres)

        def findUsersByRegex(regex: String): ZIO[ZEnv with LivePostgres, Throwable, List[User]] = {
          val q = quote {
            query[User].filter(u => sql"""${u.email} ~ $regex""".as[Boolean])
          }

          ctx.run(q).provideSomeLayer(ZLayer.succeed(ctx.database))
        }
      }
    ```
   */
  // case class Match(locator: Projection, value: String)                             extends CompareTerm[String]

  sealed trait Range: // Term[T]:

    val locator: Projection

    val reference: Interval[ValueType.VALUE]

  object Range:

//    def verify[H <: Product, T <: ValueType.VALUE](rg: Range[?])(using prj: Projectable[H]): AppResult[Unit] =
//      if prj.contains(rg.locator.path) then AppResult.Success(())
//      else AppResult.fail(s"${rg.locator.path} is not part of the type ${prj.schema.id}")

    def unapply(t: Range): (Projection, Interval[ValueType.VALUE]) = (t.locator, t.reference)

    def between[T <: ValueType.VALUE](locator: Projection, reference: Interval[T]): Range & Filter = Between(locator, reference)

    case class Between(
        override val locator: Projection,
        override val reference: Interval[ValueType.VALUE])
        extends Range
        with Filter derives MetaType

    object Between:
    end Between

  end Range // object

end Filter
