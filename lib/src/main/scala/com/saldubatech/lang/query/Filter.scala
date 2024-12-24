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

given JsonDecoder[Projection] = Projection.projectionJsonDecoder

object Filter:

  sealed trait Literal extends Filter
  object Literal:

    case object TRUE  extends Literal
    case object FALSE extends Literal

    given schema: Schema[Literal] =
      Schema
        .primitive[Boolean]
        .transform(
          {
            case true  => TRUE
            case false => FALSE
          },
          {
            case TRUE  => true
            case FALSE => false
          }
        )

    given encoder: JsonEncoder[Literal] =
      JsonEncoder.boolean.contramap[Literal] {
        case TRUE  => true
        case FALSE => false
      }

    given decoder: JsonDecoder[Literal] =
      JsonDecoder.boolean.map {
        case true  => TRUE
        case false => FALSE
      }

  end Literal

  sealed trait Composite
  case class And(clauses: List[Filter]) extends Filter, Composite derives MetaType, JsonEncoder, JsonDecoder
  case class Or(clauses: List[Filter])  extends Filter, Composite derives MetaType, JsonEncoder, JsonDecoder
  case class Not(clause: Filter)        extends Filter, Composite derives MetaType, JsonEncoder, JsonDecoder

  sealed trait Compare: // MixIn

    val locator: Projection
    val reference: ValueType.VALUE

  object Compare:

    import ValueType.given

    def unapply(t: Compare): (Projection, ValueType.VALUE) = (t.locator, t.reference)

    case class Eq(override val locator: Projection, override val reference: ValueType.VALUE) extends Filter with Compare
        derives MetaType,
          JsonEncoder,
          JsonDecoder

    case class Ne(override val locator: Projection, override val reference: ValueType.VALUE) extends Filter with Compare
        derives MetaType,
          JsonEncoder,
          JsonDecoder

    case class Gt(override val locator: Projection, override val reference: ValueType.VALUE) extends Filter with Compare
        derives MetaType,
          JsonEncoder,
          JsonDecoder

    case class Ge(override val locator: Projection, override val reference: ValueType.VALUE) extends Filter with Compare
        derives MetaType,
          JsonEncoder,
          JsonDecoder

    case class Lt(override val locator: Projection, override val reference: ValueType.VALUE) extends Filter with Compare
        derives MetaType,
          JsonEncoder,
          JsonDecoder

    case class Le(override val locator: Projection, override val reference: ValueType.VALUE) extends Filter with Compare
        derives MetaType,
          JsonEncoder,
          JsonDecoder

  end Compare

  sealed trait Range:

    val locator: Projection

    val reference: Interval[ValueType.VALUE]

  object Range:

    def unapply(t: Range): (Projection, Interval[ValueType.VALUE]) = (t.locator, t.reference)

    def between[T <: ValueType.VALUE](locator: Projection, reference: Interval[T]): Range & Filter = Between(locator, reference)

    case class Between(
        override val locator: Projection,
        override val reference: Interval[ValueType.VALUE])
        extends Range
        with Filter derives MetaType, JsonEncoder, JsonDecoder

  end Range // object

end Filter

sealed trait Filter derives MetaType, JsonEncoder, JsonDecoder:

end Filter // trait
