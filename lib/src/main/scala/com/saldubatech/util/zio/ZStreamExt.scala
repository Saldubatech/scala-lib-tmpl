package com.saldubatech.util.zio

import zio.stream.{Stream, UStream, ZStream}
import zio.*

import java.util as ju
import scala.concurrent.duration.FiniteDuration

object ZStreamExt:

  extension (zso: ZStream.type) def fromGenerator[J](initJ: J)(g: (Int, J) => Option[J]): UStream[J] =
    ZStream.unfold[(Int, J), (Int, J)](0 -> initJ){
      (n, j) => g(n, j).map{ rj => ((n,  j), ((n+1) -> rj)) }
    }.map( _._2)

  extension [R, E, A](zs: ZStream[R, E, A])
    def withBatchedSchedule(batchSize: Int, sch: Schedule[R, Any, Any]): ZStream[R, E, A] =
      zs.rechunk(batchSize).chunks.schedule(sch).flattenChunks

object ZSinkExt

object Examples:
  import ZStreamExt.*

  val gen = ZStream.fromGenerator[Int](0)((n, v) => if v < 10 then Some(v*v) else None)


