package com.saldubatech.infrastructure.storage.rdbms.quill

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.saldubatech.infrastructure.storage.{JournalEntry, NotFoundError, TimeCoordinates}
import com.saldubatech.infrastructure.storage.rdbms.datasource.DataSourceBuilder
import com.saldubatech.lang.Id
import com.saldubatech.lang.query.{Filter, Order, OrderDirection, OrderTerm, Page, Projectable, Projection}
import com.saldubatech.lang.types.*
import com.saldubatech.test.persistence.postgresql.{PostgresContainer, TestPGDataSourceBuilder}
import io.getquill.*
import io.getquill.jdbczio.Quill
import io.getquill.jdbczio.Quill.Postgres
import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*

import javax.sql.DataSource

object DynamicSampleJournalNowViewpointSpec extends ZIOSpecDefault:

  import Projectable.given

  val containerLayer: ZLayer[Any, Throwable, PostgreSQLContainer] = ZLayer.scoped(PostgresContainer.make())

  val dataSourceLayer: ZLayer[DataSourceBuilder, Nothing, DataSource] = ZLayer(ZIO.service[DataSourceBuilder].map(_.dataSource))

  val postgresLayer: ZLayer[DataSource, Nothing, Postgres[Literal.type]] = Quill.Postgres.fromNamingStrategy(Literal)

  val underTestLayer = SampleJournal.layer

  val dpf: DynamicPredicateFactory[SamplePayload] = new DynamicPredicateFactory[SamplePayload]
  val dps: DynamicSortFactory[SamplePayload]      = new DynamicSortFactory[SamplePayload]

  val probeId1: Id = Id
  val probeId2: Id = Id
  val probeId3: Id = Id
  val probeId4: Id = Id
  val probe1       = SamplePayload("first item", 1.0)
  val probe2       = SamplePayload("second item", 2.0)
  val probe3       = SamplePayload("third item", 3.0)
  val updated3     = SamplePayload("updated item", 32.0)
  val probe4       = SamplePayload("fourth item", 4.0)

  val tc0       = TimeCoordinates(0L, 0L)
  val tc1       = TimeCoordinates(1L, 1L)
  val tc2       = TimeCoordinates(2L, 2L)
  val tc3       = TimeCoordinates(3L, 3L)
  val tc32      = TimeCoordinates(13L, 13L)
  val tc4       = TimeCoordinates(4L, 4L)
  val tc4r      = TimeCoordinates(14L, 14L)
  val viewPoint = TimeCoordinates(1000L, 1000L)

  override def spec: Spec[TestEnvironment & Scope, Throwable] =
    suite("item repository test with postgres test container")(
      test("save items returns their ids") {
        for {
          underTest <- ZIO.service[SampleJournal]
          it1       <- underTest.add(probeId1, probe1, tc1)
          it2       <- underTest.add(probeId2, probe2, tc2)
          it3       <- underTest.add(probeId3, probe3, tc3)
          items     <- underTest.findAll(viewPoint)
        } yield assert(items)(hasSize(equalTo(3)))
          && assert(it1.payload)(equalTo(probe1))
          && assert(it2.payload)(equalTo(probe2))
          && assert(it3.payload)(equalTo(probe3))
          && assert(it1.eId)(equalTo(probeId1))
          && assert(it2.eId)(equalTo(probeId2))
          && assert(it3.eId)(equalTo(probeId3))
          && assert(it1.coordinates)(equalTo(tc1))
          && assert(it2.coordinates)(equalTo(tc2))
          && assert(it3.coordinates)(equalTo(tc3))
      },
      test("get all returns 3 items") {
        for {
          underTest <- ZIO.service[SampleJournal]
          items     <- underTest.findAll(viewPoint)
        } yield assert(items)(hasSize(equalTo(3)))
      },
      test("Removal can only be done once") {
        for {
          underTest <- ZIO.service[SampleJournal]
          _         <- underTest.add(probeId4, probe4, tc4)
          it4       <- underTest.remove(probeId4, tc4r)
          notFound  <- underTest.get(probeId4, viewPoint).exit
        } yield assert(it4.eId)(equalTo(probeId4))
          && assert(notFound)(fails(equalTo(NotFoundError(probeId4))))
      },
      test("update item 3") {
        for {
          qCtx      <- ZIO.service[Quill.Postgres[Literal]]
          underTest <- ZIO.service[SampleJournal]
          item      <- underTest.update(probeId3, tc32, updated3)
          rItem     <- underTest.get(probeId3, viewPoint)
        } yield assert(item.payload.name)(equalTo("updated item"))
          && assert(item.payload.price)(equalTo(32.0))
          && assert(rItem)(equalTo(item))
      },
      test("Find one element by name") {
        val eqDyn: Filter.Compare.Eq = Filter.Compare.Eq(Projection("name"), "second item")
        for {
          underTest <- ZIO.service[SampleJournal]
          dq        <- dpf.journaled(eqDyn).toZIO
          found     <- underTest.findDynamicSorted(dq, viewPoint, None)
        } yield assert(found)(hasSize(equalTo(1)))
          && assert(found.head.eId)(equalTo(probeId2))
      },
      test("Find two elements with an or condition") {
        val twoOr = Filter.Or(
          List(
            Filter.Compare.Eq(Projection("name"), "second item"),
            Filter.Compare.Lt(Projection("price"), 2.0)
          )
        )
        for {
          underTest <- ZIO.service[SampleJournal]
          dq        <- dpf.journaled(twoOr).toZIO
          found     <- underTest.findDynamicSorted(dq, viewPoint, None)
        } yield assert(found)(hasSize(equalTo(2)))
          && assert(found.map(r => r.payload))(contains(probe1) && contains(probe2))
      },
      test("Find two sorted elements with an or condition dynamically") {
        val twoOr = Filter.Or(
          List(
            Filter.Compare.Eq(Projection("name"), "second item"),
            Filter.Compare.Lt(Projection("price"), 2.0)
          )
        )
        val sort = Order(List(OrderTerm(Projection("name"), OrderDirection.Desc)))
        for {
          underTest <- ZIO.service[SampleJournal]
          dq        <- dpf.journaled(twoOr).toZIO
          ds        <- dps(sort).toZIO
          found     <- underTest.findDynamicSorted(dq, viewPoint, Some(ds))
        } yield assert(found)(hasSize(equalTo(2)))
          && assert(found.map(r => r.payload))(contains(probe1) && contains(probe2))
          && assert(found.map(r => r.payload).head)(equalTo(probe2))
      },
      test("Find two sorted elements with an or condition dynamically limited by pagination") {
        val threeOr = Filter.Or(
          List(
            Filter.Compare.Eq(Projection("name"), updated3.name),
            Filter.Compare.Eq(Projection("name"), probe2.name),
            Filter.Compare.Lt(Projection("price"), 2.0)
          )
        )
        val sort = Order(List(OrderTerm(Projection("name"), OrderDirection.Desc)))
        for {
          underTest <- ZIO.service[SampleJournal]
          dq        <- dpf.journaled(threeOr).toZIO
          ds        <- dps(sort).toZIO
          found     <- underTest.findDynamicSorted(dq, viewPoint, Some(ds), Page(1, 2))
        } yield assert(found)(hasSize(equalTo(2)))
          && assert(found.map(r => r.payload))(contains(updated3) && contains(probe2))
          && assert(found.map(r => r.payload).head)(equalTo(updated3))
      },
      test("Find one element with an and condition") {
        val oneAnd = Filter.And(
          List(
            Filter.Compare.Ne(Projection("name"), "first item"),
            Filter.Or(
              List(
                Filter.Compare.Eq(Projection("name"), "second item"),
                Filter.Compare.Lt(Projection("price"), 2.0)
              )
            )
          )
        )
        for {
          underTest <- ZIO.service[SampleJournal]
          dq        <- dpf.journaled(oneAnd).toZIO
          found     <- underTest.findDynamicSorted(dq, viewPoint, None)
        } yield assert(found)(hasSize(equalTo(1)))
          && assert(found.map(_.payload))(contains(probe2))
      },
      test("Find one element with an negated condition") {
        val oneNot = Filter.Not(
          Filter.Or(
            List(
              Filter.Compare.Eq(Projection("name"), "second item"),
              Filter.Compare.Lt(Projection("price"), 2.0)
            )
          )
        )
        for {
          quill     <- ZIO.service[Quill.Postgres[Literal]]
          underTest <- ZIO.service[SampleJournal]
          dq        <- dpf.journaled(oneNot).toZIO
          found     <- underTest.findDynamicSorted(dq, viewPoint, None)
        } yield assert(found)(hasSize(equalTo(1)))
          && assert(found.map(_.payload))(contains(updated3))
      }
    ).provideShared(
      containerLayer, TestPGDataSourceBuilder.layer, dataSourceLayer, postgresLayer, underTestLayer
    ) @@ sequential
