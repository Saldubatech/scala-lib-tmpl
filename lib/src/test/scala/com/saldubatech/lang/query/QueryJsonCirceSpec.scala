package com.saldubatech.lang.query

import com.saldubatech.lang.types.meta.MetaType
import com.saldubatech.test.BaseSpec
import zio.json.{JsonDecoder, JsonEncoder}
//import io.circe.parser.decode
//import io.circe.Codec

class QueryJsonCirceSpec extends BaseSpec:

  val underTest1 = Filter.Compare.Eq(Projection(Projectable.Field("the_path_thing")), "asdf")
  val underTest2 = Filter.Compare.Eq(Projection(Projectable.Field("the_path_thing")), 111)
  val underTest3 = Filter.Range.between(Projection(Projectable.Field("the_path_thing")), Interval("aaaa", "zzzzz"))
  val one        = Filter.Compare.Eq(Projection(Projectable.Field("the_path_thing")), "asdf")
  val two        = Filter.Compare.Eq(Projection(Projectable.Field("the_path_thing")), 111)
  val three      = Filter.Range.between(Projection(Projectable.Field("the_path_thing")), Interval("aaaa", "zzzzz"))
  val four       = Filter.Compare.Lt(Projection(Projectable.Field("adfasdf")), 33.45)
  val underTest4 = Filter.And(List(Filter.Or(List(one, two)), three, four))

  // val filterCodec: Codec[Filter] = ??? // summon[DomainType[Filter]]
  val encoder: JsonEncoder[Filter] = summon[JsonEncoder[Filter]]
  val decoder: JsonDecoder[Filter] = summon[JsonDecoder[Filter]]

  "A Query" when {
    "encoded" should {
      "print Json for Eq[String]" in {
        val rs = encoder.encodeJson(underTest1)
        println(s"### Eq[String]: underTest. $rs")
      }
      "print Json for Eq[Int]" in {
        val rs = encoder.encodeJson(underTest2)
        println(s"### Eq[Int]: underTest. $rs")
      }
      "print Json for Interval[String]" in {
        val rs = encoder.encodeJson(underTest3)
        println(s"### Between[String]: underTest. $rs")
      }
      "print Json for a Composed Filter" in {
        val rs = encoder.encodeJson(underTest4)
        println(s"### Composite: underTest. $rs")
      }
    }
    "decoded" should {
      "retrieve an Eq[Int]" in {
        val probe =
          """
            |{
            |  "Eq" : {
            |    "locator" : "the_path_thing",
            |    "reference" : 111
            |  }
            |}
            |""".stripMargin
        val rs = decoder.decodeJson(probe)
        rs shouldBe Right(underTest2)
        println(s"### Recovered Eq: underTest. $rs")
      }
      "retrieve an Eq[String]" in {
        val probe =
          """
            |{
            |  "Eq" : {
            |    "locator" : "the_path_thing",
            |    "reference" : "asdf"
            |  }
            |}
            |""".stripMargin
        val rs = decoder.decodeJson(probe)
        rs shouldBe Right(underTest1)
        println(s"### Recovered Eq: underTest. $rs")
      }
      "retrieve an Interval[String]" in {
        val probe =
          """
            |{
            |  "Between" : {
            |    "locator" : "the_path_thing",
            |    "reference" : {
            |      "min" : "aaaa",
            |      "max" : "zzzzz",
            |      "minClosed" : true,
            |      "maxClosed" : false
            |    }
            |  }
            |}
            |""".stripMargin
        val rs = decoder.decodeJson(probe)
        rs shouldBe Right(underTest3)
        println(s"### Recovered Eq: underTest. $rs")
      }
      "retrieve a composite Filter" in {
        val probe =
          """
            |{
            |  "And" : {
            |    "clauses" : [
            |      {
            |        "Or" : {
            |          "clauses" : [
            |            {
            |              "Eq" : {
            |                "locator" : "the_path_thing",
            |                "reference" : "asdf"
            |              }
            |            },
            |            {
            |              "Eq" : {
            |                "locator" : "the_path_thing",
            |                "reference" : 111
            |              }
            |            }
            |          ]
            |        }
            |      },
            |      {
            |        "Between" : {
            |          "locator" : "the_path_thing",
            |          "reference" : {
            |            "min" : "aaaa",
            |            "max" : "zzzzz",
            |            "minClosed" : true,
            |            "maxClosed" : false
            |          }
            |        }
            |      },
            |      {
            |        "Lt" : {
            |          "locator" : "adfasdf",
            |          "reference" : 33.45
            |        }
            |      }
            |    ]
            |  }
            |}
            |""".stripMargin
        val rs = decoder.decodeJson(probe)
        rs shouldBe Right(underTest4)
        println(s"### Recovered Eq: underTest. $rs")
      }
    }
  }

end QueryJsonCirceSpec
