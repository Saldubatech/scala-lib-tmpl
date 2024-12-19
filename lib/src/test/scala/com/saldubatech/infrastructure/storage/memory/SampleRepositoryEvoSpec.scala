package com.saldubatech.infrastructure.storage.memory

import com.saldubatech.infrastructure.storage.{InsertionError, NotFoundError, Predicate, Projection}
import com.saldubatech.lang.Id
import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.sequential
import zio.test.junit.JUnitRunnableSpec

object Harness:
  val underTest: InMemoryItemService = InMemoryItemService()

end Harness // object

object SampleRepositoryEvoSpec extends JUnitRunnableSpec:

  import Harness.*

  val probeId1: Id = Id
  val probeId2: Id = Id
  val probeId3: Id = Id
  val probeId4: Id = Id
  val probe1       = ItemEvo(probeId1, "first item", BigDecimal(1))
  val probe2       = ItemEvo(probeId2, "second item", BigDecimal(2))
  val probe3       = ItemEvo(probeId3, "third item", BigDecimal(3))
  val updated3     = ItemEvo(probeId3, "updated item", BigDecimal(32))
  val probe4       = ItemEvo(probeId4, "fourth item", BigDecimal(4))

  override def spec =
    suite("In Memory item repository test with")(
      test("save items returns their ids") {
        for {
          it1   <- underTest.add(probe1)
          it2   <- underTest.add(probe2)
          it3   <- underTest.add(probe3)
          items <- underTest.findAll
        } yield assert(it1.rId)(equalTo(probeId1))
          && assert(it2.rId)(equalTo(probeId2))
          && assert(it3.rId)(equalTo(probeId3))
          && assert(items)(hasSize(equalTo(3)))
      },
      test("Cannot add twice") {
        for {
          cantAdd <- underTest.add(ItemEvo(probeId1, "something else", BigDecimal(33))).exit
        } yield assert(cantAdd)(fails(equalTo(InsertionError(s"Cannot Insert record with duplicate Id: $probeId1"))))
      },
      test("get all returns 3 items") {
        for {
          items <- underTest.findAll
        } yield assert(items)(hasSize(equalTo(3)))
      },
      test("get non existing item") {
        for {
          item <- underTest.get("nothing-nothing").exit
        } yield assert(item)(fails(equalTo(NotFoundError("nothing-nothing"))))
      },
      test("Removal can only be done once") {
        for {
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
          _    <- underTest.update(updated3)
          item <- underTest.get(probeId3)
        } yield assert(item.name)(equalTo("updated item")) &&
          assert(item.price)(equalTo(BigDecimal(32)))
      },
      test("Cannot update non existing item") {
        val newId = Id
        for {
          cantAdd <- underTest.update(ItemEvo(newId, "something else", BigDecimal(33))).exit
        } yield assert(cantAdd)(fails(equalTo(NotFoundError(newId))))
      },
      test("Find one element by name") {
        for {
          found <- underTest.find(it => it.name == "second item")
        } yield assert(found)(hasSize(equalTo(1))) && assert(found.head.rId)(equalTo(probeId2))
      },
      test("Find two elements with an or condition") {
        for {
          found <- underTest.find(Predicate.or(_.name == "second item", _.price < 2.0))
        } yield assert(found)(hasSize(equalTo(2)))
          && assert(found)(contains(probe1) && contains(probe2))
      },
      test("Find one element with an and condition") {
        for {
          found <- underTest.find(Predicate.and(_.name != "first item", Predicate.or(_.name == "second item", _.price < 2.0)))
        } yield assert(found)(hasSize(equalTo(1)))
          && assert(found)(contains(probe2))
      },
      test("Find one element with an negated condition") {
        for {
          found <- underTest.find(Predicate.not(Predicate.or(_.name == "second item", _.price < 2.0)))
        } yield assert(found)(hasSize(equalTo(1)))
          && assert(found)(contains(updated3))
      },
      test("Find one element with a projected condition") {
        for {
          found <- underTest.find(Projection.project(_.name, _ == "first item"))
        } yield assert(found)(hasSize(equalTo(1)))
          && assert(found)(contains(probe1))
      }
    ) @@ sequential
