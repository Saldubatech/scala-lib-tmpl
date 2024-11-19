package com.saldubatech.infrastructure.storage.rdbms.quill

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.saldubatech.infrastructure.storage.{
  InsertionError,
  JournaledDomain,
  NotFoundError,
  Predicate,
  TimeCoordinates,
  ValidationError
}
import com.saldubatech.infrastructure.storage.rdbms.datasource.DataSourceBuilder
import com.saldubatech.lang.Id
import com.saldubatech.test.persistence.postgresql.{PostgresContainer, TestPGDataSourceBuilder}
import io.getquill.*
import io.getquill.jdbczio.Quill
import io.getquill.jdbczio.Quill.Postgres
import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*

import javax.sql.DataSource

object SampleJournalNowViewpointSpec extends ZIOSpecDefault:

  val containerLayer: ZLayer[Any, Throwable, PostgreSQLContainer] = ZLayer.scoped(PostgresContainer.make())

  val dataSourceLayer: ZLayer[DataSourceBuilder, Nothing, DataSource] = ZLayer(ZIO.service[DataSourceBuilder].map(_.dataSource))

  val postgresLayer: ZLayer[DataSource, Nothing, Postgres[Literal.type]] = Quill.Postgres.fromNamingStrategy(Literal)

  val underTestLayer = SampleJournal.layer

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
      test("Cannot add twice") {
        for {
          underTest <- ZIO.service[SampleJournal]
          cantAdd   <- underTest.add(probeId1, SamplePayload("something else", 33.0), tc1.plus(1L, 1L)).exit
        } yield assert(cantAdd)(fails(equalTo(InsertionError(s"Entity with id $probeId1 already exists"))))
      },
      test("get all returns 3 items") {
        for {
          underTest <- ZIO.service[SampleJournal]
          items     <- underTest.findAll(viewPoint)
        } yield assert(items)(hasSize(equalTo(3)))
      },
      test("get non existing item") {
        for {
          underTest <- ZIO.service[SampleJournal]
          item      <- underTest.get("nothing-nothing").exit
        } yield assert(item)(fails(equalTo(NotFoundError("nothing-nothing"))))
      },
      test("Removal can only be done once") {
        for {
          underTest  <- ZIO.service[SampleJournal]
          _          <- underTest.add(probeId4, probe4, tc4)
          it4        <- underTest.remove(probeId4, tc4r)
          notFound   <- underTest.get(probeId4, viewPoint).exit
          cantRemove <- underTest.remove(probeId4, tc4r.plus(1L, 1L)).exit
        } yield assert(it4.eId)(equalTo(probeId4))
          && assert(notFound)(fails(equalTo(NotFoundError(probeId4))))
          && assert(cantRemove)(fails(equalTo(NotFoundError(probeId4))))
      },
      test("Cannot Remove with same time coordinates") {
        for {
          qCtx       <- ZIO.service[Quill.Postgres[Literal]]
          underTest  <- ZIO.service[SampleJournal]
          itemResult <- underTest.remove(probeId3, tc3).exit
        } yield assert(itemResult)(
          fails(equalTo(ValidationError("Cannot remove unless recordedAt and effectiveAt are monotonic")))
        )
      },
      test("Cannot Update with same time coordinates") {
        for {
          qCtx       <- ZIO.service[Quill.Postgres[Literal]]
          underTest  <- ZIO.service[SampleJournal]
          itemResult <- underTest.update(probeId3, tc3, updated3).exit
        } yield assert(itemResult)(
          fails(equalTo(ValidationError("Cannot update unless recordedAt and effectiveAt are monotonic")))
        )
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
      test("Cannot update non existing item") {
        val newId = Id
        for {
          underTest <- ZIO.service[SampleJournal]
          cantAdd   <- underTest.update(newId, tc32, SamplePayload("something else", 33.0)).exit
        } yield assert(cantAdd)(fails(equalTo(NotFoundError(newId))))
      },
      test("Find one element by name") {
        for {
          quill     <- ZIO.service[Quill.Postgres[Literal]]
          underTest <- ZIO.service[SampleJournal]
          found <-
            import quill.*
            underTest.find(item => item.name == "second item", viewPoint)
        } yield assert(found)(hasSize(equalTo(1)))
          && assert(found.head.eId)(equalTo(probeId2))
      },
      test("Find two elements with an or condition") {
        for {
          quill     <- ZIO.service[Quill.Postgres[Literal]]
          underTest <- ZIO.service[SampleJournal]
          found <-
            import quill.*
            underTest.find(Predicate.or(_.name == "second item", _.price < 2.0), viewPoint)
        } yield assert(found)(hasSize(equalTo(2)))
          && assert(found.map(r => r.payload))(contains(probe1) && contains(probe2))
      },
      test("Find one element with an and condition") {
        for {
          quill     <- ZIO.service[Quill.Postgres[Literal]]
          underTest <- ZIO.service[SampleJournal]
          found <-
            import quill.*
            underTest.find(
              Predicate.and(_.name != "first item", Predicate.or(_.name == "second item", _.price < 2.0)),
              viewPoint
            )
        } yield assert(found)(hasSize(equalTo(1)))
          && assert(found.map(_.payload))(contains(probe2))
      },
      test("Find one element with an negated condition") {
        for {
          quill     <- ZIO.service[Quill.Postgres[Literal]]
          underTest <- ZIO.service[SampleJournal]
          found <-
            import quill.*
            underTest.find(Predicate.not(Predicate.or(_.name == "second item", _.price < 2.0)), viewPoint)
        } yield assert(found)(hasSize(equalTo(1)))
          && assert(found.map(_.payload))(contains(updated3))
      },
      test("Find one element with a projected condition") {
        for {
          quill     <- ZIO.service[Quill.Postgres[Literal]]
          underTest <- ZIO.service[SampleJournal]
          found <-
            import quill.*
            underTest.find(Predicate.project(r => r.name, n => n == "first item"), viewPoint)
        } yield assert(found)(hasSize(equalTo(1)))
          && assert(found.map(_.payload))(contains(probe1))
      }
    ).provideShared(
      containerLayer, TestPGDataSourceBuilder.layer, dataSourceLayer, postgresLayer, underTestLayer
    ) @@ sequential
