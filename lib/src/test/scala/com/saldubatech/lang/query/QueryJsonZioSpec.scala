package com.saldubatech.lang.query

import com.saldubatech.lang.query.Projectable.Field
import com.saldubatech.lang.types.meta.MetaType
import zio.http.codec.HttpContentCodec
import zio.test.{assertCompletes, Spec, ZIOSpecDefault}
import zio.ZIO
import zio.http.{Body, Request}
import zio.test.TestAspect.sequential

object QueryJsonZioSpec extends ZIOSpecDefault:

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
  import QueryContentCodecs.*

  val filterCodec: HttpContentCodec[Filter] = HttpContentCodec.fromSchema[Filter]
  import ValueType.given
  val valueTypeCodec: HttpContentCodec[ValueType.VALUE] = HttpContentCodec.fromSchema[ValueType.VALUE]

  override def spec =
    suite("A Query encoded should")(
      test("print Json for Eq[String]") {
        for {
          rs000 <-  valueTypeCodec.encode(12345678901234567890.1).fold(ZIO.fail(_), _.asString)
          bdy    <- wkwCodec.encode(12345678901234567890.1).fold(ZIO.fail(_), b => ZIO.succeed(b))
          rs00   <- wkwCodec.encode(12345678901234567890.1).fold(ZIO.fail(_), _.asString)
          rs0    <- wkwCodec.decodeRequest(Request.post("asdf", bdy))
          rs1    <- filterCodec.encode(underTest2).fold(err => ZIO.fail(s"Encoding Failure: $err"), s => ZIO.succeed(s))
          rsStr1 <- rs1.asString
          rs2    <- filterCodec.encode(underTest1).fold(err => ZIO.fail(s"Encoding Failure: $err"), s => ZIO.succeed(s))
          rsStr2 <- rs2.asString
          rs3    <- filterCodec.encode(underTest3).fold(err => ZIO.fail(s"Encoding Failure: $err"), s => ZIO.succeed(s))
          rsStr3 <- rs3.asString
          rs4    <- filterCodec.encode(underTest4).fold(err => ZIO.fail(s"Encoding Failure: $err"), s => ZIO.succeed(s))
          rsStr4 <- rs4.asString
        } yield
          println(s"#### NOMINAL ENCODED: <<$rs000>>")
          println(s"#### ENCODED: <<$rs00>>")
          println(s"#### DECODED: <<${rs0.getClass}>>")
          println(s"#### Eq[Int]: underTest. $rsStr1")
          println(s"#### Eq[String]: underTest. $rsStr2")
          println(s"#### Between[String]: underTest. $rsStr3")
          println(s"#### Composite: underTest. $rsStr4")
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

end QueryJsonZioSpec
