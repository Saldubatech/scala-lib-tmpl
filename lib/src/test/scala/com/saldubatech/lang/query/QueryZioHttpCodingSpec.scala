package com.saldubatech.lang.query

import com.saldubatech.lang.query.Projectable.Field
import com.saldubatech.lang.types.meta.MetaType
import zio.http.codec.HttpContentCodec
import zio.ZIO
import zio.http.{Body, Request}
import zio.test.*
import zio.test.TestAspect.sequential
import zio.test.Assertion.*

object QueryZioHttpCodingSpec extends ZIOSpecDefault:

  import MetaType.given

  val underTest1 = Filter.Compare.Eq(Projection(Projectable.Field("the_path_thing")), "asdf")
  val underTest2 = Filter.Compare.Eq(Projection(Projectable.Field("the_path_thing")), 111)
  val underTest3 = Filter.Range.between(Projection(Projectable.Field("the_path_thing")), Interval("aaaa", "zzzzz"))
  val one        = Filter.Compare.Eq(Projection(Projectable.Field("the_path_thing")), "asdf")
  val two        = Filter.Compare.Eq(Projection(Projectable.Field("the_path_thing")), 111)
  val three      = Filter.Range.between(Projection(Projectable.Field("the_path_thing")), Interval("aaaa", "zzzzz"))
  val four       = Filter.Compare.Lt(Projection(Projectable.Field("adfasdf")), 33.45)
  val underTest4 = Filter.And(List(Filter.Or(List(one, two)), three, four))

  import com.saldubatech.infrastructure.network.oas3.entity.QueryContentCodecs
  import QueryContentCodecs.given
//  import QueryContentCodecs.*
  import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

  override def spec =
    suite("A Query encoded should")(
      test("print Json for Value") {
        for {
          rs000 <- valueTypeCodec.encode(12345678901234567890.1).fold(ZIO.fail(_), _.asString)

        } yield
          assert(rs000)(equalTo("12345678901234567890.1"))
          assertCompletes
      },
      test("print Json for Eq[String]") {
        for {
          rs000  <- valueTypeCodec.encode(12345678901234567890.1).fold(ZIO.fail(_), _.asString)
          rs1    <- filterCodec.encode(underTest2).fold(err => ZIO.fail(s"Encoding Failure: $err"), s => ZIO.succeed(s))
          rsStr1 <- rs1.asString
          rs2    <- filterCodec.encode(underTest1).fold(err => ZIO.fail(s"Encoding Failure: $err"), s => ZIO.succeed(s))
          rsStr2 <- rs2.asString
          rs3    <- filterCodec.encode(underTest3).fold(err => ZIO.fail(s"Encoding Failure: $err"), s => ZIO.succeed(s))
          rsStr3 <- rs3.asString
          rs4    <- filterCodec.encode(underTest4).fold(err => ZIO.fail(s"Encoding Failure: $err"), s => ZIO.succeed(s))
          rsStr4 <- rs4.asString
        } yield
          assert(rs000)(equalTo("12345678901234567890.1"))
          assert(rsStr1)(equalTo(s"""{"Eq":{"locator":"the_path_thing","reference":"111"}}"""))
          assert(rsStr2)(equalTo(s"""{"Eq":{"locator":"the_path_thing","reference":"asdf"}}"""))
          assert(rsStr3)(equalTo(s"""{"Between":{"locator":[{"Field":"the_path_thing"}],"reference":{"min":{"string":"aaaa"},"max":{"string":"zzzzz"},"minClosed":true,"maxClosed":false}}}"""))
          assert(rsStr4)(equalTo(s"""{"Composite":{"And":{"clauses":[{"Composite":{"Or":{"clauses":[{"Eq":{"locator":[{"Field":"the_path_thing"}],"reference":{"string":"asdf"}}},{"Eq":{"locator":[{"Field":"the_path_thing"}],"reference":{"int":111}}}]}}},{"Between":{"locator":[{"Field":"the_path_thing"}],"reference":{"min":{"string":"aaaa"},"max":{"string":"zzzzz"},"minClosed":true,"maxClosed":false}}},{"Lt":{"locator":[{"Field":"adfasdf"}],"reference":{"double":33.45}}}]}}}"""))
          assertCompletes
      },
      test("Locator Roundtrip") {
        val fieldProbe = Projectable.Field("the_path_thing")
        val indexProbe = Projectable.Index(333)
        val probe      = List(fieldProbe, indexProbe, fieldProbe)
//        val locatorCodec = HttpContentCodec.fromSchema(Projectable.locatorSchema)
        for {
          bodyField         <- fieldCodec.encode(fieldProbe).fold(ZIO.fail(_), ZIO.succeed(_))
          encodedField      <- bodyField.asString
          decodedField      <- fieldCodec.decodeRequest(Request.post("/", bodyField))
          bodyIndex         <- indexCodec.encode(indexProbe).fold(ZIO.fail(_), ZIO.succeed(_))
          encodedIndex      <- bodyIndex.asString
          decodedIndex      <- indexCodec.decodeRequest(Request.post("/", bodyIndex))
          bodyStep          <- stepCodec.encode(fieldProbe).fold(ZIO.fail(_), ZIO.succeed(_))
          encodedStep       <- bodyStep.asString
          decodedStep       <- fieldCodec.decodeRequest(Request.post("/", bodyStep))
          bodyLocator       <- locatorCodec.encode(probe).fold(ZIO.fail(_), ZIO.succeed(_))
          encodedLocator    <- bodyLocator.asString
          decodedLocator    <- locatorCodec.decodeRequest(Request.post("/", bodyLocator))
          bodyProjection    <- projectionCodec.encode(Projection(probe)).fold(ZIO.fail(_), ZIO.succeed(_))
          encodedProjection <- bodyProjection.asString
          decodedProjection <- projectionCodec.decodeRequest(Request.post("/", bodyProjection))
        } yield
          println(s"#### Field:      $encodedField, ${encodedField.length}")
          println(s"#### Index:      $encodedIndex")
          println(s"#### Step:       $encodedStep")
          println(s"#### Locator:    $encodedLocator")
          println(s"#### Projection: $encodedProjection")
          assert(decodedField)(equalTo(fieldProbe))
          assert(decodedIndex)(equalTo(indexProbe))
          assert(decodedStep)(equalTo(fieldProbe))
          assert(decodedLocator)(equalTo(probe))
          assert(decodedProjection)(equalTo(Projection(probe)))
          assertCompletes
      },
      test("Filter Roundtrip") {
        for {
          bodyFilter    <- filterCodec.encode(underTest4).fold(ZIO.fail(_), ZIO.succeed(_))
          encodedFilter <- bodyFilter.asString
          decodedFilter <- filterCodec.decodeRequest(Request.post("/", bodyFilter))
        } yield
          println(s"#### Encoded Filter: $encodedFilter")
          assert(decodedFilter)(equalTo(underTest4))
          assertCompletes
      }
    ) @@ sequential

//    "decoded" should {
//      "retrieve an Eq[Int]" in {
//        val probe =
//          """
//            |{
//            |  "Eq" : {
//            |    "locator" : "the_path_thing",
//            |    "reference" : 111
//            |  }
//            |}
//            |""".stripMargin
//        val rs = filterCodec.decode(probe)
//        rs shouldBe Right(underTest2)
//        println(s"### Recovered Eq: underTest. $rs")
//      }
//      "retrieve an Eq[String]" in {
//        val probe =
//          """
//            |{
//            |  "Eq" : {
//            |    "locator" : "the_path_thing",
//            |    "reference" : "asdf"
//            |  }
//            |}
//            |""".stripMargin
//        val rs = decode[Filter](probe)
//        rs shouldBe Right(underTest1)
//        println(s"### Recovered Eq: underTest. $rs")
//      }
//      "retrieve an Interval[String]" in {
//        val probe =
//          """
//            |{
//            |  "Between" : {
//            |    "locator" : "the_path_thing",
//            |    "reference" : {
//            |      "min" : "aaaa",
//            |      "max" : "zzzzz",
//            |      "minClosed" : true,
//            |      "maxClosed" : false
//            |    }
//            |  }
//            |}
//            |""".stripMargin
//        val rs = decode[Filter](probe)
//        rs shouldBe Right(underTest3)
//        println(s"### Recovered Eq: underTest. $rs")
//      }
//      "retrieve a composite Filter" in {
//        val probe =
//          """
//            |{
//            |  "And" : {
//            |    "clauses" : [
//            |      {
//            |        "Or" : {
//            |          "clauses" : [
//            |            {
//            |              "Eq" : {
//            |                "locator" : "the_path_thing",
//            |                "reference" : "asdf"
//            |              }
//            |            },
//            |            {
//            |              "Eq" : {
//            |                "locator" : "the_path_thing",
//            |                "reference" : 111
//            |              }
//            |            }
//            |          ]
//            |        }
//            |      },
//            |      {
//            |        "Between" : {
//            |          "locator" : "the_path_thing",
//            |          "reference" : {
//            |            "min" : "aaaa",
//            |            "max" : "zzzzz",
//            |            "minClosed" : true,
//            |            "maxClosed" : false
//            |          }
//            |        }
//            |      },
//            |      {
//            |        "Lt" : {
//            |          "locator" : "adfasdf",
//            |          "reference" : 33.45
//            |        }
//            |      }
//            |    ]
//            |  }
//            |}
//            |""".stripMargin
//        val rs = decode[Filter](probe)
//        rs shouldBe Right(underTest4)
//        println(s"### Recovered Eq: underTest. $rs")
//      }
//    }

end QueryZioHttpCodingSpec
