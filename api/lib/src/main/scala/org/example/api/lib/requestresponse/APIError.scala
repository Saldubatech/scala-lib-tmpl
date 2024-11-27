package org.example.api.lib.requestresponse

import com.saldubatech.infrastructure.storage.{InsertionError, NotFoundError, ValidationError}
import com.saldubatech.lang.types.AppResult
import com.saldubatech.lang.types.datetime.Epoch
import org.example.api.lib.bindings.ServiceEndpoint
import zio.http.*
import zio.http.codec.*
import zio.http.endpoint.{AuthType, Endpoint}
import zio.schema.{DeriveSchema, Schema}

import scala.reflect.ClassTag

sealed trait APIError:

  val requestId: Long
  val from: ServiceEndpoint
  val at: Epoch
  val message: APIError.Message
  val status: Status

end APIError // trait

/** See https://en.wikipedia.org/wiki/List_of_HTTP_status_codes */
object APIError:

  type Message = String // in the future maybe move to Json or "Any" with a specialized codec

  case class BadRequest400(
      override val requestId: Long,
      override val from: ServiceEndpoint,
      override val at: Epoch,
      override val message: Message)
      extends APIError:
    override val status = Status.BadRequest

  case class Unauthorized401(
      override val requestId: Long,
      override val from: ServiceEndpoint,
      override val at: Epoch,
      override val message: Message)
      extends APIError:
    override val status = Status.Unauthorized

  type NonAuthenticated403 = Unauthorized401

  case class PaymentRequired402(
      override val requestId: Long,
      override val from: ServiceEndpoint,
      override val at: Epoch,
      override val message: Message)
      extends APIError:
    override val status = Status.PaymentRequired

  type QuotaExhausted402 = PaymentRequired402

  case class NotFound404(
      override val requestId: Long,
      override val from: ServiceEndpoint,
      override val at: Epoch,
      override val message: Message)
      extends APIError:
    override val status = Status.NotFound

  case class Conflict409(
      override val requestId: Long,
      override val from: ServiceEndpoint,
      override val at: Epoch,
      override val message: Message)
      extends APIError:
    override val status = Status.Conflict

  type InvalidUpdate409 = Conflict409

  case class PreconditionRequired429(
      override val requestId: Long,
      override val from: ServiceEndpoint,
      override val at: Epoch,
      override val message: Message)
      extends APIError:
    override val status = Status.PreconditionRequired

  type StaleUpdate429 = PreconditionRequired429

  case class UnavailableForLegalReasons451(
      override val requestId: Long,
      override val from: ServiceEndpoint,
      override val at: Epoch,
      override val message: Message)
      extends APIError:
    override val status = Status.Custom(451, "Unavailable For Legal Reasons (RFC 7725)")

  case class InternalServerError500(
      override val requestId: Long,
      override val from: ServiceEndpoint,
      override val at: Epoch,
      override val message: Message)
      extends APIError:
    override val status = Status.InternalServerError

  case class NotImplemented501(
      override val requestId: Long,
      override val from: ServiceEndpoint,
      override val at: Epoch,
      override val message: Message)
      extends APIError:
    override val status = Status.NotImplemented

  val schema                            = DeriveSchema.gen[APIError]
  val codec: HttpContentCodec[APIError] = HttpContentCodec.fromSchema(schema)

  implicit def schemaResolver[E <: APIError](using ct: ClassTag[E]): Schema[E] =
    ct.runtimeClass match
      case rtC if rtC == classOf[BadRequest400]           => DeriveSchema.gen[BadRequest400].asInstanceOf[Schema[E]]
      case rtC if rtC == classOf[Unauthorized401]         => DeriveSchema.gen[Unauthorized401].asInstanceOf[Schema[E]]
      case rtC if rtC == classOf[PaymentRequired402]      => DeriveSchema.gen[PaymentRequired402].asInstanceOf[Schema[E]]
      case rtC if rtC == classOf[NotFound404]             => DeriveSchema.gen[NotFound404].asInstanceOf[Schema[E]]
      case rtC if rtC == classOf[Conflict409]             => DeriveSchema.gen[Conflict409].asInstanceOf[Schema[E]]
      case rtC if rtC == classOf[PreconditionRequired429] => DeriveSchema.gen[PreconditionRequired429].asInstanceOf[Schema[E]]
      case rtC if rtC == classOf[UnavailableForLegalReasons451] =>
        DeriveSchema.gen[UnavailableForLegalReasons451].asInstanceOf[Schema[E]]
      case rtC if rtC == classOf[InternalServerError500] => DeriveSchema.gen[InternalServerError500].asInstanceOf[Schema[E]]
      case rtC if rtC == classOf[NotImplemented501]      => DeriveSchema.gen[NotImplemented501].asInstanceOf[Schema[E]]

  implicit def codec[E <: APIError: ClassTag]: HttpContentCodec[E] = HttpContentCodec.fromSchema(schemaResolver[E])

  class Mapper(from: ServiceEndpoint):

    def map(rId: Long, at: Epoch, appError: AppResult.Error): APIError =
      appError match
        case nfErr: NotFoundError  => NotFound404(rId, from, at, s"No result found for Id: ${nfErr.id}")
        case vErr: ValidationError => BadRequest400(rId, from, at, vErr.msg)
        case iErr: InsertionError  => Conflict409(rId, from, at, iErr.msg)
        case other                 => InternalServerError500(rId, from, at, other.msg)

  end Mapper

end APIError // object

extension [Value](codecEnum: HttpCodec.Enumeration[Value])

  def f9[
      AtomTypes,
      Sub1 <: Value: ClassTag,
      Sub2 <: Value: ClassTag,
      Sub3 <: Value: ClassTag,
      Sub4 <: Value: ClassTag,
      Sub5 <: Value: ClassTag,
      Sub6 <: Value: ClassTag,
      Sub7 <: Value: ClassTag,
      Sub8 <: Value: ClassTag,
      Sub9 <: Value: ClassTag
    ](codec1: HttpCodec[AtomTypes, Sub1],
      codec2: HttpCodec[AtomTypes, Sub2],
      codec3: HttpCodec[AtomTypes, Sub3],
      codec4: HttpCodec[AtomTypes, Sub4],
      codec5: HttpCodec[AtomTypes, Sub5],
      codec6: HttpCodec[AtomTypes, Sub6],
      codec7: HttpCodec[AtomTypes, Sub7],
      codec8: HttpCodec[AtomTypes, Sub8],
      codec9: HttpCodec[AtomTypes, Sub9]
    ): HttpCodec[AtomTypes, Value] =
    (codec1 | codec2 | codec3 | codec4 | codec5 | codec6 | codec7 | codec8 | codec9).transformOrFail(either =>
      Right(
        either.left
          .map(
            _.left.map(_.left.map(_.left.map(_.left.map(_.left.map(_.left.map(_.merge).merge).merge).merge).merge).merge).merge
          )
          .merge
      )
    ) {
      case sub1: Sub1 => Right(Left(Left(Left(Left(Left(Left(Left(Left(sub1)))))))))
      case sub2: Sub2 => Right(Left(Left(Left(Left(Left(Left(Left(Right(sub2)))))))))
      case sub3: Sub3 => Right(Left(Left(Left(Left(Left(Left(Right(sub3))))))))
      case sub4: Sub4 => Right(Left(Left(Left(Left(Left(Right(sub4)))))))
      case sub5: Sub5 => Right(Left(Left(Left(Left(Right(sub5))))))
      case sub6: Sub6 => Right(Left(Left(Left(Right(sub6)))))
      case sub7: Sub7 => Right(Left(Left(Right(sub7))))
      case sub8: Sub8 => Right(Left(Right(sub8)))
      case sub9: Sub9 => Right(Right(sub9))
      case _          => Left(s"Unexpected error type")
    }

extension [PathInput, Input, Err, Output, Auth <: AuthType, Err2](
    oer: Endpoint.OutErrors[PathInput, Input, Err, Output, Auth, Err2]
  )

  def with9[
      Sub1 <: Err2: ClassTag,
      Sub2 <: Err2: ClassTag,
      Sub3 <: Err2: ClassTag,
      Sub4 <: Err2: ClassTag,
      Sub5 <: Err2: ClassTag,
      Sub6 <: Err2: ClassTag,
      Sub7 <: Err2: ClassTag,
      Sub8 <: Err2: ClassTag,
      Sub9 <: Err2: ClassTag
    ](codec1: HttpCodec[HttpCodecType.Status & HttpCodecType.Content, Sub1],
      codec2: HttpCodec[HttpCodecType.Status & HttpCodecType.Content, Sub2],
      codec3: HttpCodec[HttpCodecType.Status & HttpCodecType.Content, Sub3],
      codec4: HttpCodec[HttpCodecType.Status & HttpCodecType.Content, Sub4],
      codec5: HttpCodec[HttpCodecType.Status & HttpCodecType.Content, Sub5],
      codec6: HttpCodec[HttpCodecType.Status & HttpCodecType.Content, Sub6],
      codec7: HttpCodec[HttpCodecType.Status & HttpCodecType.Content, Sub7],
      codec8: HttpCodec[HttpCodecType.Status & HttpCodecType.Content, Sub8],
      codec9: HttpCodec[HttpCodecType.Status & HttpCodecType.Content, Sub9]
    )(implicit alt: Alternator[Err2, Err]
    ): Endpoint[PathInput, Input, alt.Out, Output, Auth] = {
    val codec = HttpCodec.enumeration.f9(codec1, codec2, codec3, codec4, codec5, codec6, codec7, codec8, codec9)
    oer.self.copy[PathInput, Input, alt.Out, Output, Auth](error = codec | oer.self.error)
  }

extension [PathInput, Input, Output, Auth <: AuthType](
    ep: zio.http.endpoint.Endpoint[PathInput, Input, zio.ZNothing, Output, Auth]
  )

  inline def standardErrors = // : Endpoint[PathInput, Input, Error, Output, Auth] =
    import APIError.{*, given}
    given HttpContentCodec[APIError] = APIError.codec
    ep
      .outErrors[APIError]
      .with9(
        HttpCodec.error[BadRequest400](Status.BadRequest),
        HttpCodec.error[Unauthorized401](Status.Unauthorized),
        HttpCodec.error[PaymentRequired402](Status.PaymentRequired),
        HttpCodec.error[NotFound404](Status.NotFound),
        HttpCodec.error[Conflict409](Status.Conflict),
        HttpCodec.error[PreconditionRequired429](Status.PreconditionRequired),
        HttpCodec.error[UnavailableForLegalReasons451](Status.Custom(451, "Unavailable For Legal Reasons (RFC 7725)")),
        HttpCodec.error[InternalServerError500](Status.InternalServerError),
        HttpCodec.error[NotImplemented501](Status.NotImplemented)
      )
