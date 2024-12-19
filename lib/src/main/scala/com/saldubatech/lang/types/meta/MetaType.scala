package com.saldubatech.lang.types.meta

import io.circe.{Codec, Decoder, Encoder}
import zio.schema.{DeriveSchema, Deriver, Schema, StandardType}
import zio.Chunk
import zio.schema.Deriver.WrappedF

import scala.quoted.{Expr, Quotes, Type}

trait MetaType[D]: //  extends Codec[D]:

  val zioSchema: Schema[D]
//  val circeCodec: Codec[D]
//  export circeCodec.*

object MetaType:

  // transparent inline def derived[D](using mD: deriving.Mirror.Of[D]): DomainType[D] = ${ deriveDomainType[D](using 'mD) }
  transparent inline def derived[D]: MetaType[D] = ${ deriveDomainType[D] }

//  private def deriveDomainType[D](using mD: Expr[deriving.Mirror.Of[D]])(using d: Type[D])(using Quotes): Expr[DomainType[D]] =
  private def deriveDomainType[D](using d: Type[D])(using q: Quotes): Expr[MetaType[D]] =
    '{
      import com.saldubatech.lang.query.ValueType.given
      new MetaType[d.Underlying] {
        import MetaType.given
        override val zioSchema = DeriveSchema.gen[d.Underlying]
      }
    }

  implicit def zioSchema[D](using d: MetaType[D]): Schema[D] = d.zioSchema

end MetaType
