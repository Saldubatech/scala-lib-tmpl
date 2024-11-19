package com.saldubatech.infrastructure.storage.rdbms.quill

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.saldubatech.infrastructure.storage.rdbms.datasource.DataSourceBuilder
import com.saldubatech.infrastructure.storage.{InsertionError, NotFoundError, Predicate}
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

object SampleRepositoryEvoSpec extends ZIOSpecDefault:

  val containerLayer: ZLayer[Any, Throwable, PostgreSQLContainer] = ZLayer.scoped(PostgresContainer.make())

  val dataSourceLayer: ZLayer[DataSourceBuilder, Nothing, DataSource] = ZLayer(ZIO.service[DataSourceBuilder].map(_.dataSource))

  val postgresLayer: ZLayer[DataSource, Nothing, Postgres[Literal.type]] = Quill.Postgres.fromNamingStrategy(Literal)

  val underTestLayer = ItemService.layer

  val probeId1: Id = Id
  val probeId2: Id = Id
  val probeId3: Id = Id
  val probeId4: Id = Id
  val probe1       = ItemEvo(probeId1, "first item", BigDecimal(1))
  val probe2       = ItemEvo(probeId2, "second item", BigDecimal(2))
  val probe3       = ItemEvo(probeId3, "third item", BigDecimal(3))
  val updated3     = ItemEvo(probeId3, "updated item", BigDecimal(32))
  val probe4       = ItemEvo(probeId4, "fourth item", BigDecimal(4))

  override def spec: Spec[TestEnvironment & Scope, Throwable] =
    suite("item repository test with postgres test container")(
      test("save items returns their ids") {
        for {
          underTest <- ZIO.service[ItemService]
          it1       <- underTest.add(probe1)
          it2       <- underTest.add(probe2)
          it3       <- underTest.add(probe3)
          items     <- underTest.findAll
        } yield assert(it1.rId)(equalTo(probeId1))
          && assert(it2.rId)(equalTo(probeId2))
          && assert(it3.rId)(equalTo(probeId3))
          && assert(items)(hasSize(equalTo(3)))
      },
      test("Cannot add twice") {
        for {
          underTest <- ZIO.service[ItemService]
          cantAdd   <- underTest.add(ItemEvo(probeId1, "something else", BigDecimal(33))).exit
        } yield assert(cantAdd)(fails(equalTo(InsertionError(s"Cannot Insert record with duplicate Id: $probeId1"))))
      },
      test("get all returns 3 items") {
        for {
          underTest <- ZIO.service[ItemService]
          items     <- underTest.findAll
        } yield assert(items)(hasSize(equalTo(3)))
      },
      test("get non existing item") {
        for {
          underTest <- ZIO.service[ItemService]
          item      <- underTest.get("nothing-nothing").exit
        } yield assert(item)(fails(equalTo(NotFoundError("nothing-nothing"))))
      },
      test("Removal can only be done once") {
        for {
          underTest  <- ZIO.service[ItemService]
          it4Add     <- underTest.add(probe4)
          it4        <- underTest.remove(probeId4)
          notFound   <- underTest.get(probeId4).exit
          cantRemove <- underTest.remove(probeId4).exit
        } yield assert(it4.rId)(equalTo(probeId4))
          && assert(notFound)(fails(equalTo(NotFoundError(probeId4))))
          && assert(cantRemove)(fails(equalTo(NotFoundError(probeId4))))
      },
      test("update item 3") {
        for {
          underTest <- ZIO.service[ItemService]
          item      <- underTest.update(updated3)
          rItem     <- underTest.get(probeId3)
        } yield assert(item.name)(equalTo("updated item")) &&
          assert(item.price)(equalTo(BigDecimal(32))) &&
          assert(rItem)(equalTo(item))
      },
      test("Cannot update non existing item") {
        val newId = Id
        for {
          underTest <- ZIO.service[ItemService]
          cantAdd   <- underTest.update(ItemEvo(newId, "something else", BigDecimal(33))).exit
        } yield assert(cantAdd)(fails(equalTo(NotFoundError(newId))))
      },
      test("Find one element by name") {
        for {
          quill     <- ZIO.service[Quill.Postgres[Literal]]
          underTest <- ZIO.service[ItemService]
          found <-
            import quill.*
            underTest.find(item => item.name == "second item")
        } yield assert(found)(hasSize(equalTo(1))) && assert(found.head.rId)(equalTo(probeId2))
      },
      test("Find two elements with an or condition") {
        for {
          quill     <- ZIO.service[Quill.Postgres[Literal]]
          underTest <- ZIO.service[ItemService]
          found <-
            import quill.*
            underTest.find(Predicate.or(_.name == "second item", _.price < lift(BigDecimal(2))))
        } yield assert(found)(hasSize(equalTo(2)))
          && assert(found)(contains(probe1) && contains(probe2))
      },
      test("Find one element with an and condition") {
        for {
          quill     <- ZIO.service[Quill.Postgres[Literal]]
          underTest <- ZIO.service[ItemService]
          found <-
            import quill.*
            underTest.find(
              Predicate.and(_.name != "first item", Predicate.or(_.name == "second item", _.price < lift(BigDecimal(2.0))))
            )
        } yield assert(found)(hasSize(equalTo(1)))
          && assert(found)(contains(probe2))
      },
      test("Find one element with an negated condition") {
        for {
          quill     <- ZIO.service[Quill.Postgres[Literal]]
          underTest <- ZIO.service[ItemService]
          found <-
            import quill.*
            underTest.find(Predicate.not(Predicate.or(_.name == "second item", _.price < lift(BigDecimal(2.0)))))
        } yield assert(found)(hasSize(equalTo(1)))
          && assert(found)(contains(updated3))
      },
      test("Find one element with a projected condition") {
        for {
          quill     <- ZIO.service[Quill.Postgres[Literal]]
          underTest <- ZIO.service[ItemService]
          found <-
            import quill.*
            underTest.find(Predicate.project(r => r.name, n => n == "first item"))
        } yield assert(found)(hasSize(equalTo(1)))
          && assert(found)(contains(probe1))
      }
    ).provideShared(
      containerLayer, TestPGDataSourceBuilder.layer, dataSourceLayer, postgresLayer, underTestLayer
    ) @@ sequential
