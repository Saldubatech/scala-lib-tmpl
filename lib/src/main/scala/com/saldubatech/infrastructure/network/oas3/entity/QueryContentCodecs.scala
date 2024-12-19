package com.saldubatech.infrastructure.network.oas3.entity

import com.saldubatech.lang.query.Projectable.{Field, Index, Step}
import com.saldubatech.lang.query.Projection
import zio.http.codec.*
import zio.schema.codec.{BinaryCodec, JsonCodec}
import zio.schema.{DeriveSchema, Schema}

object QueryContentCodecs:

  given fieldSchema: Schema[Field]                  = Schema.primitive[String].transform[Field](str => Field(str), f => "PPPP" + f.name)
  given fieldCodec: HttpContentCodec[Field]         = HttpContentCodec.fromSchema(fieldSchema)
  given indexSchema: Schema[Index]                  = Schema.primitive[Int].transform[Index](i => Index(i), i => i.index)
  given indexCodec: HttpContentCodec[Index]         = HttpContentCodec.fromSchema(indexSchema)
  given stepSchema: Schema[Step]                    = DeriveSchema.gen[Step]
  given stepCodec: HttpContentCodec[Step]           = HttpContentCodec.fromSchema(stepSchema)
  val projectionSchema: Schema[Projection]          = DeriveSchema.gen[Projection]
  val projectionCodec: HttpContentCodec[Projection] = HttpContentCodec.fromSchema(projectionSchema)

  val listStepSchema = DeriveSchema.gen[List[Step]]
  val listStepCodec  = HttpContentCodec.fromSchema(listStepSchema)

  given projectionStepSchema: Schema[Projection] =
    DeriveSchema
      .gen[List[Step]]
      .transform[Projection](
        path => Projection(path),
        prj => prj.path
      )

  given projectionStepCodec: HttpContentCodec[Projection] = HttpContentCodec.fromSchema(projectionStepSchema)

  val fEi: Schema[String | Int] = fieldSchema
    .orElseEither(indexSchema)
    .transform(
      {
        case Left(f)  => f.name
        case Right(i) => i.index
      },
      {
        case i: Int    => Right(Index(i))
        case s: String => Left(Field(s))
      }
    )

  val fEiCodec = HttpContentCodec.fromSchema(fEi)

  val strSchema  = Schema.primitive[String]
  val intSchema  = Schema.primitive[Int]
  val boolSchema = Schema.primitive[Boolean]

  val wkw0 = strSchema.transformOrFail[Boolean | Int | Long | Float | Double | String](
    str =>
      str.toBooleanOption match
        case Some(b) => Right(b)
        case None    => Right("PUCK" + str),
    {
      case str: String => Right(str)
      case other       => Right("SPOILER" + other.toString)
    }
  )

  val wkw = strSchema.transformOrFail[Boolean | Int | Long | Float | Double | String](
    str =>
      str.toBooleanOption.fold(
        str.toIntOption.fold(
          str.toLongOption.fold(
//            str.toFloatOption.fold(
              str.toDoubleOption.fold(Right(str))(d => Right(d))
//            )(f => Right(f))
          )(l => Right(l))
        )(i => Right(i))
      )(b => Right(b)),
    {
      case str: String => Right(str)
      case other       => Right(other.toString)
    }
  )

  val wkwCodec = HttpContentCodec.fromSchema(wkw)

end QueryContentCodecs
