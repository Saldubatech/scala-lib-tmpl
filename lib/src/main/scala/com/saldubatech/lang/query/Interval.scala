package com.saldubatech.lang.query

import com.saldubatech.lang.types.meta.MetaType
//import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}
//import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import zio.json.{JsonDecoder, JsonEncoder}
import zio.schema.{DeriveSchema, Schema}

import scala.reflect.Typeable

case class Interval[+T](min: T, max: T, minClosed: Boolean = true, maxClosed: Boolean = false)
    derives MetaType,
      JsonEncoder,
      JsonDecoder

end Interval

object Interval:

//  def apply[T <: ValueType.VALUE: Typeable](min: T, max: T, minClosed: Boolean = true, maxClosed: Boolean = false): Interval[T] =
//    min match
//      case _: Int     => IntervalInt(min.asInstanceOf[Int], max.asInstanceOf[Int], minClosed, maxClosed)
//      case _: Long    => IntervalLong(min.asInstanceOf[Long], max.asInstanceOf[Long], minClosed, maxClosed)
//      case _: Float   => IntervalFloat(min.asInstanceOf[Float], max.asInstanceOf[Float], minClosed, maxClosed)
//      case _: Double  => IntervalDouble(min.asInstanceOf[Double], max.asInstanceOf[Double], minClosed, maxClosed)
//      case _: String  => IntervalString(min.asInstanceOf[String], max.asInstanceOf[String], minClosed, maxClosed)
//      case _: Boolean => IntervalBool(min.asInstanceOf[Boolean], max.asInstanceOf[Boolen], minClosed, maxClosed)
//
//  case class IntervalInt(
//      override val min: Int,
//      override val max: Int,
//      override val minClosed: Boolean = true,
//      override val maxClosed: Boolean = false)
//      extends Interval[Int]
//
//  case class IntervalLong(
//      override val min: Long,
//      override val max: Long,
//      override val minClosed: Boolean = true,
//      override val maxClosed: Boolean = false)
//      extends Interval[Long]
//
//  case class IntervalFloat(
//      override val min: Float,
//      override val max: Float,
//      override val minClosed: Boolean = true,
//      override val maxClosed: Boolean = false)
//      extends Interval[Float]
//
//  case class IntervalDouble(
//      override val min: Double,
//      override val max: Double,
//      override val minClosed: Boolean = true,
//      override val maxClosed: Boolean = false)
//      extends Interval[Double]
//
//  case class IntervalString(
//      override val min: String,
//      override val max: String,
//      override val minClosed: Boolean = true,
//      override val maxClosed: Boolean = false)
//      extends Interval[String]
//
//  case class IntervalBool(
//      override val min: Boolean,
//      override val max: Boolean,
//      override val minClosed: Boolean = true,
//      override val maxClosed: Boolean = false)
//      extends Interval[Boolean]

  import ValueType.given
  import io.circe.syntax.*

  implicit def schema[T: Schema]: Schema[Interval[T]] = DeriveSchema.gen[Interval[T]]

//  implicit def encoder[V <: ValueType.VALUE: Typeable]: Encoder[Interval[V]] = deriveEncoder
//  implicit def decoder[V <: ValueType.VALUE: Typeable]: Decoder[Interval[V]] = deriveDecoder

//  implicit def encoder[V <: ValueType.VALUE]: Encoder[Interval[V]] =
//    new Encoder[Interval[V]] {
//      def apply(v: Interval[V]): Json =
//        Json.obj(
//          "min"       -> v.min.asInstanceOf[ValueType.VALUE].asJson,
//          "max"       -> v.max.asInstanceOf[ValueType.VALUE].asJson,
//          "minClosed" -> v.minClosed.asJson,
//          "maxClosed" -> v.maxClosed.asJson
//        )
//    }

//  implicit def decoder[V <: ValueType.VALUE: Typeable]: Decoder[Interval[V]] =
//    new Decoder[Interval[V]]:
//      override def apply(c: HCursor): Decoder.Result[Interval[V]] =
//        c.value.fold[Decoder.Result[Interval[V]]](
//          jsonNull = Left(DecodingFailure("Value cannot be null", c.history)),
//          jsonBoolean = b => Left(DecodingFailure(s"$b is not an Interval object", c.history)),
//          jsonNumber = jN => Left(DecodingFailure(s"$jN is not an Interval object", c.history)),
//          jsonString = s => Left(DecodingFailure(s"$s is not an Interval object", c.history)),
//          jsonArray = l => Left(DecodingFailure(s"$l is not an Interval object", c.history)),
//          jsonObject = o =>
//            for {
//              min <- o("min").fold(Left(DecodingFailure(s"no Minimum Value in $o", c.history)))(r => r.as[ValueType.VALUE])
//              max <- o("max").fold(Left(DecodingFailure(s"no Maximum Value in $o", c.history)))(r => r.as[ValueType.VALUE])
//              mnC <- Right(o("minClosed").flatMap(_.asBoolean).fold(true)(r => r))
//              mxC <- Right(o("maxClosed").flatMap(_.asBoolean).fold(false)(r => r))
//              itv <- (min, max) match
//                       case (mn: V, mx: V) => Right(Interval[V](mn, mx, mnC, mxC))
//                       case other =>
//                         Left(DecodingFailure("Cannot build an interval from: $o because argument types don't match", c.history))
//            } yield itv
//        )

end Interval // object
