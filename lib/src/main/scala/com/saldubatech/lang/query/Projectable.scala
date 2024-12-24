package com.saldubatech.lang.query

import com.saldubatech.lang.types.*
import com.saldubatech.lang.types.meta.MetaType
import zio.http.codec.HttpContentCodec
import zio.json.*
import zio.json.internal.{RetractReader, Write}
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

  private val arrayIndexRegex   = """(\[(\d+)])""".r
  private val fieldRegex        = """([a-zA-Z_][a-zA-Z_0-9]*)""".r
  private val indexOrFieldRegEx = s"""($arrayIndexRegex|$fieldRegex)""".r
  val stringyLocatorRegExp      = s"$indexOrFieldRegEx((\\.$indexOrFieldRegEx)*)".r

  sealed trait Step:
    def stringify: String
  end Step // trait

  object Step:

    given jsonDecoder: JsonDecoder[Step] =
      JsonDecoder.string.mapOrFail(str =>
        if arrayIndexRegex.matches(str) then Index.jsonDecoder.decodeJson(s"\"$str\"")
        else if fieldRegex.matches(str) then Field.jsonDecoder.decodeJson(s"\"$str\"")
        else Left(s"$str is not a valid locator step")
      )

    given jsonEncoder: JsonEncoder[Step] =
      JsonEncoder.string.contramap { step =>
        val chars = step match
          case f: Field => Field.jsonEncoder.encodeJson(f)
          case i: Index => Index.jsonEncoder.encodeJson(i)
        chars.subSequence(1, chars.length() - 1).toString
      }

    given schema: Schema[Step] =
      Schema[String].transformOrFail[Step](
        (str: String) => jsonDecoder.decodeJson(str),
        (step: Step) => Right(step.toJson)
      )

  end Step // object

  case class Field(name: String) extends Step: // Meta not derived b/c special encoding below.
    override def stringify: String = name

  object Field:

    given schema: Schema[Field] =
      Schema[String].transformOrFail(
        str => jsonDecoder.decodeJson(str),
        idx => Right(jsonEncoder.encodeJson(idx).toString)
      )

    given jsonEncoder: JsonEncoder[Field] = JsonEncoder.string.contramap(f => f.stringify)

    given jsonDecoder: JsonDecoder[Field] =
      JsonDecoder.string.mapOrFail(str =>
        if fieldRegex.matches(str) then Right(Field(str))
        else Left(s"$str is not a valid field name")
      )

  end Field // object

  case class Index(index: Int) extends Step: // Meta not derived b/c special encoding below
    override def stringify: String = s"[$index]"

  object Index:

    given schema: Schema[Index] =
      Schema[String].transformOrFail(
        str => jsonDecoder.decodeJson(str),
        idx => Right(jsonEncoder.encodeJson(idx).toString)
      )

    given jsonEncoder: JsonEncoder[Index] = JsonEncoder.string.contramap(idx => idx.stringify)

    given jsonDecoder: JsonDecoder[Index] =
      JsonDecoder.string.mapOrFail(str =>
        if arrayIndexRegex.matches(str) then Right(Index(str.slice(1, str.length - 2 + 1).toInt))
        else Left(s"$str is not a valid array index")
      )

  end Index // object

  implicit def f(s: String): Step = Field(s)
  implicit def i(idx: Int): Step  = Index(idx)

  type Locator = List[Step]
  implicit def loc(s: Step*): Locator          = s.toList
  extension (l: Locator) def stringify: String = l.map(s => s.stringify).mkString(".")

  // ^(\[\d+])|(([a-zA-Z_]\w*))(\.((\[\d+])|(([a-zA-Z_]\w*))))*$
  given locatorSchema: Schema[Locator] = // Schema.list(using Step.schema)
    Schema[String].transformOrFail(
      str => locatorDecoder.decodeJson(str),
      l => Right(locatorEncoder.encodeJson(l).toString)
    )

  given locatorEncoder: JsonEncoder[Locator] = JsonEncoder.string.contramap((loc: Locator) => loc.stringify)

  given locatorDecoder: JsonDecoder[Locator] =
    JsonDecoder.string.mapOrFail((str: String) =>
      if stringyLocatorRegExp.matches(str) then
        val steps: List[Either[String, Step]] =
          str.split("""\.""").map(stStr => JsonDecoder[Step].decodeJson(s"\"$stStr\"")).toList
        steps.collect { case Left(errorStr) => errorStr } match
          case Nil        => Right(steps.collect { case Right(step) => step })
          case one :: Nil => Left(one)
          case multiple =>
            Left(s"Multiple Steps could not be read: ${multiple.mkString("[", " :: ", "]")}")
      else Left(s"$str is not a valid locator path")
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
