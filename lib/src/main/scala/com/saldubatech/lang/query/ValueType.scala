package com.saldubatech.lang.query

import io.circe.generic.auto.*
import zio.schema.{Schema, StandardType, TypeId}
import zio.Chunk
import zio.json.{JsonDecoder, JsonEncoder}
import zio.json.internal.Write

import scala.reflect.Typeable

sealed trait ValueType:

end ValueType // trait

object ValueType:

  type VALUE = Int | Long | Float | Double | String | Boolean

  given valueEncoder: JsonEncoder[VALUE] =
    new JsonEncoder[VALUE] {
      override def unsafeEncode(a: VALUE, indent: Option[Int], out: Write): Unit =
        a match
          case i: Int     => JsonEncoder.int.unsafeEncode(i, indent, out)
          case l: Long    => JsonEncoder.long.unsafeEncode(l, indent, out)
          case f: Float   => JsonEncoder.float.unsafeEncode(f, indent, out)
          case d: Double  => JsonEncoder.double.unsafeEncode(d, indent, out)
          case s: String  => JsonEncoder.string.unsafeEncode(s, indent, out)
          case b: Boolean => JsonEncoder.boolean.unsafeEncode(b, indent, out)
    }

  given valueDecoder: JsonDecoder[VALUE] = {
    val decoderOrder: List[JsonDecoder[?]] = List(
      JsonDecoder.boolean, JsonDecoder.int, JsonDecoder.long, JsonDecoder.float, JsonDecoder.double
    )
    JsonDecoder.string.map[VALUE] { raw =>
      decoderOrder.iterator.map(d => d.decodeJson(raw)).collectFirst { case Right(rs) => rs } match
        case None     => raw
        case Some(rs) => rs.asInstanceOf[VALUE]
    }
  }

  private def valCaseSchema[V <: VALUE: StandardType: Typeable](name: String): Schema.Case[VALUE, V] =
    Schema.Case[VALUE, V](
      id = name,
      schema = Schema.primitive[V],
      unsafeDeconstruct = (v: VALUE) => v.asInstanceOf[V],
      construct = (v: V) => v,
      isCase = {
        case _: V => true
        case _    => false
      },
      annotations = Chunk.empty
    )

  given vSch: Schema[VALUE] =
    Schema.Enum6[Int, Long, Float, Double, String, Boolean, VALUE](
      id = TypeId.fromTypeName("ValueType.VALUE"),
      case1 = valCaseSchema[Int]("int"),
      case2 = valCaseSchema[Long]("long"),
      case3 = valCaseSchema[Float](name = "float"),
      case4 = valCaseSchema[Double](name = "double"),
      case5 = valCaseSchema[String](name = "string"),
      case6 = valCaseSchema[Boolean](name = "bool")
    )


  given Typeable[VALUE] =
    new Typeable[VALUE] {
      override def unapply(x: Any): Option[x.type & VALUE] =
        x match
          case _: Int        => Some(x).asInstanceOf[Option[x.type & VALUE]]
          case _: Long       => Some(x).asInstanceOf[Option[x.type & VALUE]]
          case _: Float      => Some(x).asInstanceOf[Option[x.type & VALUE]]
          case _: Double     => Some(x).asInstanceOf[Option[x.type & VALUE]]
          case _: BigDecimal => Some(x).asInstanceOf[Option[x.type & VALUE]]
          case _: String     => Some(x).asInstanceOf[Option[x.type & VALUE]]
          case _: Boolean    => Some(x).asInstanceOf[Option[x.type & VALUE]]
          case _             => None
    }

  inline def equality[T <: ValueType.VALUE](inline l: T, inline r: T): Boolean = l == r
  inline def ne[T <: ValueType.VALUE](inline l: T, inline r: T): Boolean       = l != r

  inline def gt[T <: ValueType.VALUE](inline l: T, inline r: T): Boolean =
    (l, r) match
      case (il: Int, ir: Int)         => il > ir
      case (il: Long, ir: Long)       => il > ir
      case (il: Float, ir: Float)     => il > ir
      case (il: Double, ir: Double)   => il > ir
      case (il: String, ir: String)   => il > ir
      case (il: Boolean, ir: Boolean) => il > ir

  inline def ge[T <: ValueType.VALUE](inline l: T, inline r: T): Boolean =
    (l, r) match
      case (il: Int, ir: Int)         => il > ir
      case (il: Long, ir: Long)       => il > ir
      case (il: Float, ir: Float)     => il > ir
      case (il: Double, ir: Double)   => il > ir
      case (il: String, ir: String)   => il > ir
      case (il: Boolean, ir: Boolean) => il > ir

  inline def lt[T <: ValueType.VALUE](inline l: T, inline r: T): Boolean =
    (l, r) match
      case (il: Int, ir: Int)         => il > ir
      case (il: Long, ir: Long)       => il > ir
      case (il: Float, ir: Float)     => il > ir
      case (il: Double, ir: Double)   => il > ir
      case (il: String, ir: String)   => il > ir
      case (il: Boolean, ir: Boolean) => il > ir

  inline def le[T <: ValueType.VALUE](inline l: T, inline r: T): Boolean =
    (l, r) match
      case (il: Int, ir: Int)         => il > ir
      case (il: Long, ir: Long)       => il > ir
      case (il: Float, ir: Float)     => il > ir
      case (il: Double, ir: Double)   => il > ir
      case (il: String, ir: String)   => il > ir
      case (il: Boolean, ir: Boolean) => il > ir

  inline def betweenOpenOpen[T <: ValueType.VALUE](inline x: T, inline min: T, inline max: T): Boolean =
    (x, min, max) match
      case (ix: Int, il: Int, ir: Int)             => ix > il && ix < ir
      case (ix: Long, il: Long, ir: Long)          => ix > il && ix < ir
      case (ix: Float, il: Float, ir: Float)       => ix > il && ix < ir
      case (ix: Double, il: Double, ir: Double)    => ix > il && ix < ir
      case (ix: String, il: String, ir: String)    => ix > il && ix < ir
      case (ix: Boolean, il: Boolean, ir: Boolean) => ix > il && ix < ir

  inline def betweenOpenClosed[T <: ValueType.VALUE](inline x: T, inline min: T, inline max: T): Boolean =
    (x, min, max) match
      case (ix: Int, il: Int, ir: Int)             => ix > il && ix <= ir
      case (ix: Long, il: Long, ir: Long)          => ix > il && ix <= ir
      case (ix: Float, il: Float, ir: Float)       => ix > il && ix <= ir
      case (ix: Double, il: Double, ir: Double)    => ix > il && ix <= ir
      case (ix: String, il: String, ir: String)    => ix > il && ix <= ir
      case (ix: Boolean, il: Boolean, ir: Boolean) => ix > il && ix <= ir

  inline def betweenClosedOpen[T <: ValueType.VALUE](inline x: T, inline min: T, inline max: T): Boolean =
    (x, min, max) match
      case (ix: Int, il: Int, ir: Int)             => ix >= il && ix < ir
      case (ix: Long, il: Long, ir: Long)          => ix >= il && ix < ir
      case (ix: Float, il: Float, ir: Float)       => ix >= il && ix < ir
      case (ix: Double, il: Double, ir: Double)    => ix >= il && ix < ir
      case (ix: String, il: String, ir: String)    => ix >= il && ix < ir
      case (ix: Boolean, il: Boolean, ir: Boolean) => ix >= il && ix < ir

  inline def betweenClosedClosed[T <: ValueType.VALUE](inline x: T, inline min: T, inline max: T): Boolean =
    (x, min, max) match
      case (ix: Int, il: Int, ir: Int)             => ix >= il && ix <= ir
      case (ix: Long, il: Long, ir: Long)          => ix >= il && ix <= ir
      case (ix: Float, il: Float, ir: Float)       => ix >= il && ix <= ir
      case (ix: Double, il: Double, ir: Double)    => ix >= il && ix <= ir
      case (ix: String, il: String, ir: String)    => ix >= il && ix <= ir
      case (ix: Boolean, il: Boolean, ir: Boolean) => ix >= il && ix <= ir

  inline def between[T <: ValueType.VALUE](
      inline x: T,
      inline min: T,
      inline max: T,
      inline minClosed: Boolean,
      inline maxClosed: Boolean
    ): Boolean =
    (minClosed, maxClosed) match
      case (true, true)   => betweenOpenOpen(x, min, max)
      case (true, false)  => betweenOpenClosed(x, min, max)
      case (false, true)  => betweenClosedOpen(x, min, max)
      case (false, false) => betweenClosedClosed(x, min, max)

end ValueType // object
