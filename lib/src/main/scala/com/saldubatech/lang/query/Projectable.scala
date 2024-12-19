package com.saldubatech.lang.query

import com.saldubatech.lang.types.*
import com.saldubatech.lang.types.meta.MetaType
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import zio.json.{JsonDecoder, JsonEncoder}
import zio.optics.{Optic, OpticFailure}
import zio.schema.{DeriveSchema, Schema}
import zio.schema.TypeId.{Nominal, Structural}

// (\[(\\d+)]|\D\w*)\.(\[(\\d+)]|\D\w*)*
// (\[\d+])|(([a-zA-Z]\w*)(\.([a-zA-Z]\w*))*)
// the_path_thing
import scala.reflect.Typeable

class Projectable[H]:

  def apply[T <: ValueType.VALUE: Typeable](h: H, path: Projectable.Locator): AppResult[T] = Projectable.resolve(h, path)

  def validate[T <: ValueType.VALUE: Typeable](h: H, path: Projectable.Locator): AppResult[Unit] = apply(h, path).unit

object Projectable:

  implicit def apply[H <: Product | Seq[?] | Map[String, ?] | ValueType.VALUE]: Projectable[H] = new Projectable[H]()

  sealed trait Step derives MetaType, JsonEncoder, JsonDecoder:
    def stringify: String

  case class Field(name: String) extends Step derives JsonEncoder: // Meta not derived b/c special encoding below.
    override def stringify: String = name

  object Field:
    given Schema[Field] = Schema[String].transform(str => Field(str), f => f.name)
  end Field // object

  case class Index(index: Int) extends Step derives JsonEncoder: // Meta not derived b/c special encoding below
    override def stringify: String = s"[$index]"

  object Index:
    given Schema[Index] = Schema[Int].transform(i => Index(i), f => f.index)
  end Index // object

  implicit def f(s: String): Step = Field(s)
  implicit def i(idx: Int): Step  = Index(idx)

  type Locator = List[Step]
  implicit def loc(s: Step*): Locator          = s.toList
  extension (l: Locator) def stringify: String = l.map(s => s.stringify).mkString(".")
  given Schema[Step]                           = DeriveSchema.gen[Step]

  given stringyStepEncoder: Encoder[Step] =
    Encoder.instance {
      case Field(nm)  => Encoder.encodeString(nm)
      case Index(idx) => Encoder.encodeString(s"[$idx]")
    }

  // ^(\[\d+])|(([a-zA-Z_]\w*))(\.((\[\d+])|(([a-zA-Z_]\w*))))*$
  private val arrayIndexRegex   = """(\[(\d+)])""".r
  private val fieldRegex        = """([a-zA-Z_]\w*)""".r
  private val indexOrFieldRegEx = s"""($arrayIndexRegex|$fieldRegex)""".r

  given stringyStepDecoder: Decoder[Step] =
    Decoder.instance { hC =>
      hC.value.asString.fold(Left(DecodingFailure("Expecting an Index or a field name", hC.history))) { str =>
        arrayIndexRegex.findFirstMatchIn(str) match
          case Some(inner) => Right(Index(inner.group(1).toInt))
          case None =>
            if fieldRegex.matches(str) then Right(Field(str))
            else Left(DecodingFailure("The given step: $str does not match an index or a field", hC.history))
      }
    }

  given Schema[Locator] = DeriveSchema.gen[Locator]

  given stringyEncoder: Encoder[Locator] =
    Encoder.instance { (l: Locator) =>
      Json.fromString(
        l.map(s => s.asJson.asString.get).mkString(".")
      )
    }

  private val stringyLocatorRegExp = s"$indexOrFieldRegEx((\\.$indexOrFieldRegEx)*)".r

  given stringyDecoder: Decoder[Locator] =
    Decoder.instance(hC =>
      hC.value.asString.fold(Left(DecodingFailure("Expecting a dot separated path", hC.history))) { str =>
        if stringyLocatorRegExp.matches(str) then
          val steps = str.split("""\.""").map(s => Decoder[Step].decodeJson(Json.fromString(s)))
          steps.collectFirst { case Left(dEx) => dEx }.toList match
            case Nil =>
              val wkw1 = steps.collect { case Right(step) => step }.toList
              Right(wkw1)
            case one :: Nil => Left(one)
            case multiple =>
              Left(
                DecodingFailure(
                  DecodingFailure.Reason.CustomReason(
                    s"Multiple Steps could not be read: ${multiple.map(dF => dF.reason.toString).mkString("::")}"
                  ),
                  multiple.flatMap(dF => dF.history)
                )
              )
        else Left(DecodingFailure(s"$str is not a valid locator path", hC.history))
      }
    )

  def resolve[H, T <: ValueType.VALUE: Typeable](h: H, path: List[Step]): AppResult[T] =
    resolveUnsafe(h, path).flatMap {
      case t: T  => AppResult.Success(t)
      case other => AppResult.fail(s"$other is not of the expected type")
    }

  private def resolveUnsafe(e: Any, path: List[Step]): AppResult[Any] =
    e match
      case e: Product =>
        productResolveUnsafe(e, path.tail)
      case e: Seq[?] =>
        seqResolveUnsafe(e, path.tail)
      case e: ValueType.VALUE =>
        if path.tail.isEmpty then AppResult.Success(e)
        else AppResult.fail(s"Reached a Primitive Value before exhausting the path")
      case e: Map[?, ?] =>
        mapResolveUnsafe(e.asInstanceOf[Map[String, Any]], path.tail)
      case other => AppResult.fail(s"$other schema is not supported")

  private def productResolveUnsafe[H <: Product](h: H, path: List[Step]): AppResult[Any] =
    path.headOption match
      case Some(Field(nm)) =>
        val idx = h.productElementNames.indexOf(nm)
        if idx >= 0 then resolveUnsafe(h.productElement(idx), path.tail)
        else AppResult.fail(s"$nm is not a field in $h or Schema is malformed")
      case Some(other) => AppResult.fail(s"$other is not a Field Step as expected for a product")
      case None        => AppResult.fail(s"Path exhausted without finding a ValueType.VALUE")

  private def mapResolveUnsafe[H <: Map[String, Any]](h: H, path: List[Step]): AppResult[Any] =
    path.headOption match
      case Some(Field(str)) =>
        h.get(str) match
          case Some(e) => resolveUnsafe(e, path.tail)
          case None    => AppResult.fail(s"$str is not a key in map $h")
      case Some(other) => AppResult.fail(s"$other is not a Field Step as expected for a product")
      case None        => AppResult.fail(s"Path exhausted without finding a ValueType.VALUE")

  private def seqResolveUnsafe[H <: Seq[?]](h: H, path: List[Step]): AppResult[Any] =
    path.headOption match
      case Some(Index(idx)) =>
        if h.sizeIs > idx then resolveUnsafe(h(idx), path.tail)
        else AppResult.fail(s"Index $idx is out of bounds")
      case Some(other) => AppResult.fail(s"$other is not an Index Step as expected for a product")
      case None        => AppResult.fail(s"Path exhausted without finding a ValueType.VALUE")

end Projectable
