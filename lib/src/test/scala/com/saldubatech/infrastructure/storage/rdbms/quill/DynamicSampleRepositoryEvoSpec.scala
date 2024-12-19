package com.saldubatech.infrastructure.storage.rdbms.quill

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.saldubatech.infrastructure.storage.{InsertionError, NotFoundError, Predicate}
import com.saldubatech.infrastructure.storage.rdbms.datasource.DataSourceBuilder
import com.saldubatech.lang.Id
import com.saldubatech.lang.query.{Filter, Projectable, Projection}
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

object DynamicSampleRepositoryEvoSpec extends ZIOSpecDefault:

  import Projectable.given
  import Projection.given

  val containerLayer: ZLayer[Any, Throwable, PostgreSQLContainer] = ZLayer.scoped(PostgresContainer.make())

  val dataSourceLayer: ZLayer[DataSourceBuilder, Nothing, DataSource] = ZLayer(ZIO.service[DataSourceBuilder].map(_.dataSource))

  val postgresLayer: ZLayer[DataSource, Nothing, Postgres[Literal.type]] = Quill.Postgres.fromNamingStrategy(Literal)

  val underTestLayer = ItemService.layer

  val dpf = new DynamicPredicateFactory[ItemEvo]()

  val probeId1: Id = Id
  val probeId2: Id = Id
  val probeId3: Id = Id
  val probeId4: Id = Id
  val probe1       = ItemEvo(probeId1, "first item", 1.0)
  val probe2       = ItemEvo(probeId2, "second item", 2.0)
  val probe3       = ItemEvo(probeId3, "third item", 3.0)
  val probe4       = ItemEvo(probeId4, "fourth item", 4.0)

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
      test("get all returns 3 items") {
        for {
          underTest <- ZIO.service[ItemService]
          items     <- underTest.findAll
        } yield assert(items)(hasSize(equalTo(3)))
      },
      test("Find one element by name") {
        val eq: Filter.Compare.Eq = Filter.Compare.Eq(Projection("name"), "second item")
        for {
          underTest <- ZIO.service[ItemService]
          dq        <- dpf(eq).toZIO
          found     <- underTest.dynamicFind(dq)
        } yield assert(found)(hasSize(equalTo(1))) && assertTrue(found.head.rId == probeId2)
      },
      test("Find two elements with an or condition") {
        val twoOr = Filter.Or(
          List(
            Filter.Compare.Eq(Projection("name"), "second item"),
            Filter.Compare.Lt(Projection("price"), 2.0)
          )
        )
        for {
          underTest <- ZIO.service[ItemService]
          dq        <- dpf(twoOr).toZIO
          found     <- underTest.dynamicFind(dq) // Predicate.or(_.name == "second item", _.price < lift(BigDecimal(2))))
        } yield assert(found)(hasSize(equalTo(2)))
          && assert(found)(contains(probe1) && contains(probe2))
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
          underTest <- ZIO.service[ItemService]
          dq        <- dpf(oneAnd).toZIO
          found     <- underTest.dynamicFind(dq)
        } yield assert(found)(hasSize(equalTo(1)))
          && assert(found)(contains(probe2))
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
          underTest <- ZIO.service[ItemService]
          dq        <- dpf(oneNot).toZIO
          found     <- underTest.dynamicFind(dq)
        } yield assert(found)(hasSize(equalTo(1)))
          && assert(found)(contains(probe3))
      }
    ).provideShared(
      containerLayer, TestPGDataSourceBuilder.layer, dataSourceLayer, postgresLayer, underTestLayer
    ) @@ sequential
