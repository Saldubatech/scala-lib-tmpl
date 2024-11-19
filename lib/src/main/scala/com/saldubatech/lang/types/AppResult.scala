package com.saldubatech.lang.types

import zio.{Cause, IO, Layer, Task, ZIO, ZLayer}

import scala.reflect.Typeable
import scala.util.chaining.scalaUtilChainingOps

// For now just an alias for Either...
type AppResult[+R] = Either[AppResult.Error, R]
type UnitResult    = AppResult[Unit]

object AppResult:

  class Error(val msg: String, val cause: Option[Throwable] = None) extends Throwable(msg, cause.orNull)
  object Error:

    def fromCause(cause: Cause[Error]): Error =
      cause match
        case Cause.Empty            => Error("Unknown Error (no cause)")
        case Cause.Fail(err, _)     => Error(err.msg, Some(err))
        case Cause.Die(th, _)       => Error("Unexpected Internal Error", Some(th))
        case Cause.Interrupt(fb, _) => Error(s"Fiber interrupted $fb")
        case Cause.Stackless(c, _)  => fromCause(c)
        case Cause.Then(l, r)       => CollectedError("Multiple Sequential Errors", List(fromCause(l), fromCause(r)))
        case Cause.Both(l, r)       => CollectedError("Multiple Parallel Errors", List(fromCause(l), fromCause(r)))

  end Error

  case class CollectedError(override val msg: String, causes: Iterable[Throwable] = Seq()) extends Error(msg)

  type Success[+R] = Right[Error, R]

  object Success:

    inline def apply[R](r: R): AppResult[R] = Right(r)

  val unit: AppResult[Unit] = Success(())

  type Fail[+R] = Left[Error, R]
  object Fail:

    inline def apply[R](e: Error): Fail[R] = Left[Error, R](e)

  inline def fail[R](msg: Option[String], cause: Option[Throwable]): AppResult[R] =
    (msg, cause) match
      case (None, None)                       => Fail[R](Error(s"Unknown Error"))
      case (_, Some(ae: Error))               => Fail[R](ae)
      case (None, cause @ Some(th))           => Fail(Error(th.getMessage, cause))
      case (Some(otherMsg), cause @ Some(th)) => Fail(Error(otherMsg, cause))
      case (Some(otherMsg), None)             => Fail(Error(otherMsg))

  inline def fail[R](msg: String): AppResult[R]                              = fail(Some(msg), None)
  inline def fail[R](cause: Throwable): AppResult[R]                         = fail(None, Some(cause))
  inline def fail[R](msg: String, cause: Option[Error] = None): AppResult[R] = Fail(Error(msg, cause))
  def Unknown[R]: AppResult[R]                                               = fail[R]("Unknown Error")

  def fromIO[A](z: SIO[A])(using rt: zio.Runtime[Any]): AppResult[A] =
    zio.Unsafe.unsafe { implicit u =>
      rt.unsafe.run(z) match
        case zio.Exit.Failure(cause) => fail(Error.fromCause(cause))
        case zio.Exit.Success(rs)    => Success(rs)
    }

end AppResult // object

extension [E <: AppResult.Error, R](rs: Either[E, R])

  def toZIO: IO[E, R] =
    rs match
      case Right(rs) => ZIO.succeed(rs)
      case Left(err) => ZIO.fail(err)

extension [R](rs: AppResult[R])

  def tapError(r: AppResult.Error => Unit): AppResult[R] =
    rs match
      case Left(err) =>
        r(err)
        rs
      case _ => rs

  def tapSuccess(s: R => Unit): AppResult[R] =
    rs match
      case Right(r) =>
        s(r)
        rs
      case _ => rs

  def unit: UnitResult = rs.map(_ => ())

  def isSuccess: Boolean = rs.isRight
  def isError: Boolean   = rs.isLeft

  def toIO: SIO[R] =
    rs match
      case Left(err) => ZIO.fail(err)
      case Right(r)  => ZIO.succeed(r)

extension [R](elements: Iterable[AppResult[R]])

  def collectAll: AppResult[Iterable[R]] =
    elements.foldLeft(AppResult.Success[List[R]](List.empty)) {
      case (Right(acc), Right(element)) => AppResult.Success(element :: acc)
      case (Right(acc), Left(err))      => AppResult.Fail(err)
      case (Left(errAcc), Right(_))     => AppResult.Fail(errAcc)
      case (Left(errAcc), Left(err)) =>
        err match
          case AppResult.CollectedError(msg, causes) =>
            AppResult.Fail(AppResult.CollectedError("Multiple Errors", causes.toList :+ err))
          case other => AppResult.Fail(AppResult.CollectedError("Multiple Errors", List(other, err)))
    }

  def collectAny: AppResult[Iterable[R]] =
    elements.foldLeft(AppResult.Success[List[R]](List.empty)) {
      case (Right(acc), Right(element))   => AppResult.Success(element :: acc)
      case (Right(acc), Left(err))        => AppResult.Success(acc)
      case (Left(errAcc), Right(element)) => AppResult.Success(List(element))
      case (Left(errAcc), Left(err)) =>
        err match
          case AppResult.CollectedError(msg, causes) =>
            AppResult.Fail(AppResult.CollectedError("Multiple Errors", causes.toList :+ err))
          case other => AppResult.Fail(AppResult.CollectedError("Multiple Errors", List(other, err)))
    }

  def collectAtLeastOne: AppResult[Iterable[R]] =
    for {
      l          <- elements.collectAny
      atLeastOne <- if l.isEmpty then AppResult.fail(s"No valid results") else AppResult.Success(l)
    } yield atLeastOne

// Specialized ZIO Effects with the "S" to signify "Salduba"

type SZIO[-R, +E <: AppResult.Error, +A] = ZIO[R, E, A]
type SRIO[-R, +A]                        = ZIO[R, AppResult.Error, A]
type STask[+A]                           = SRIO[Any, A]
type SIO[+A]                             = IO[AppResult.Error, A]

// Specialized ZIO Layers

type SZLayer[-RIn, +E <: AppResult.Error, +ROut] = ZLayer[RIn, E, ROut]
type SRLayer[-RIn, +ROut]                        = ZLayer[RIn, AppResult.Error, ROut]
type SLayer[+E <: AppResult.Error, +ROut]        = Layer[E, ROut]
type STaskLayer[+ROut]                           = SRLayer[Any, ROut]
