package com.saldubatech.infrastructure.network.oas3.entity

import com.saldubatech.infrastructure.network.oas3.{APIError, Adaptor as Oas3Adaptor}
import com.saldubatech.infrastructure.services.{Entity, EntityService}
import com.saldubatech.lang.query.{Page, Query}
import com.saldubatech.lang.types.{toZIO, DIO}
import com.saldubatech.lang.types.datetime.Epoch
import com.saldubatech.lang.Id
import com.saldubatech.lang.types.meta.MetaType
import zio.IO

object Adaptor:

  class Summarizer[E <: Entity, S: MetaType](h: Adaptor[E, S], summarize: E => S):

    def handleQuerySummaries(q: Query, requestId: Long): IO[APIError, (PageResult[S], Long)] =
      h.handleQuery(q, requestId)
        .map((pr, rId) => (pr.mapResults(tE => EntityResult(tE.id, summarize(tE.payload))), rId))

    def handleFindSummaries(q: String, requestId: Long): IO[APIError, (PageResult[S], Long)] =
      h.handleFind(q, requestId)
        .map((pr, rId) => (pr.mapResults(tE => EntityResult(tE.id, summarize(tE.payload))), rId))

end Adaptor // object

trait Adaptor[E <: Entity, S] extends Oas3Adaptor:

  override val service: EntityService[E]

  def handleQuery(q: Query, requestId: Long): IO[APIError, (PageResult[E], Long)] =
    service.query(q).paginatedResponse(requestId, q)

  def handleFind(q: String, requestId: Long): IO[APIError, (PageResult[E], Long)] =
    Query.fromJson(q).toZIO.mapError(errorHandler.map(requestId, Epoch.now, _)).flatMap(iQ => handleQuery(iQ, requestId))

  def handleGet(id: String, requestId: Long): IO[APIError, (EntityResult[E], Long)] = service.get(id).oneEntityResponse(requestId)

  def handleCreate(newE: E, requestId: Long): IO[APIError, (EntityResult[E], Long)] =
    service.create(newE).oneEntityResponse(requestId)

  def handleDelete(id: String, requestId: Long): IO[APIError, (EntityResult[E], Long)] =
    service.delete(id).oneEntityResponse(requestId)

  def handleUpdate(eId: String, newE: E, requestId: Long): IO[APIError, (EntityResult[E], Long)] =
    service.update(newE).oneEntityResponse(requestId)

  def handleQuerySummaries(q: Query, requestId: Long)(using s: Adaptor.Summarizer[E, S]): IO[APIError, (PageResult[S], Long)] =
    s.handleQuerySummaries(q, requestId)

  def handleFindSummaries(q: String, requestId: Long)(using s: Adaptor.Summarizer[E, S]): IO[APIError, (PageResult[S], Long)] =
    s.handleFindSummaries(q, requestId)

  extension (raw: DIO[E])

    def oneEntityResponse(requestId: Long): IO[APIError, (EntityResult[E], Long)] =
      raw.mapBoth(
        err => errorHandler.map(requestId, Epoch.now, err),
        rs => (EntityResult(EntityResult.Id(serviceLocator, rs.locator.localId), rs), requestId)
      )

  extension (raw: DIO[Iterable[E]])

    def paginatedResponse(requestId: Long, q: Query): IO[APIError, (PageResult[E], Long)] =
      raw.mapBoth(
        err => errorHandler.map(requestId, Epoch.now, err),
        { success =>
          (
            PageResult[E](
              q.encoded,
              q.previousPage.encoded,
              q.encoded,
              success.map(t => EntityResult(EntityResult.Id(serviceLocator, t.locator.localId), t)).toList
            ),
            requestId
          )
        }
      )

    def prOld(requestId: Long, q: Query) =
      raw.mapBoth(
        errorHandler.map(requestId, Epoch.now, _),
        success =>
          if success.size < q.page.size then (PageResult(q.encoded, q.previousPage.encoded, q.encoded, List()), requestId)
          else
            (
              PageResult(
                q.encoded,
                q.previousPage.encoded,
                q.nextPage.encoded,
                success.map(t => EntityResult(EntityResult.Id(serviceLocator, t.locator.localId), t)).toList
              ),
              requestId
            )
      )

end Adaptor
