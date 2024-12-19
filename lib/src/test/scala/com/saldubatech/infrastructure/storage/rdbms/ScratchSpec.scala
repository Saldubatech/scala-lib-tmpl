package com.saldubatech.infrastructure.storage.rdbms

import com.saldubatech.infrastructure.storage.Sort
import com.saldubatech.infrastructure.storage.rdbms.Scratch.schemaMetaExt
import com.saldubatech.infrastructure.storage.JournaledDomain.EntryRecord
import com.saldubatech.lang.types.*
import com.saldubatech.lang.Id
import com.saldubatech.lang.types.datetime.Epoch
import io.getquill.*
import zio.{Scope, ZIO}
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*

import scala.reflect.runtime.universe.{typeOf, TypeTag}
import scala.reflect.Typeable

object ScratchSpec extends ZIOSpecDefault:

  case class O(aa: String, bb: Long)
  case class P(x: String, a: Int, b: Boolean, o1: O)

  val ctx = new SqlMirrorContext(MirrorSqlDialect, Literal)

  import ctx.*

  // inline def probeSch: Quoted[EntityQuery[P]] = quote(querySchema[P]("p_table"))
  implicit val oSchM: SchemaMeta[O]             = schemaMeta[O]("OOOO")
  val uu                                        = "p_o1"
  implicit val pSchM: SchemaMeta[P]             = schemaMeta[P]("DUDU")
  implicit val rSch: SchemaMeta[EntryRecord[P]] = schemaMeta[EntryRecord[P]]("BLAHHH")

  val probe = P("asdf", 33, true, O("asdf", 33L))

  override def spec: Spec[TestEnvironment & Scope, Throwable] =
    suite("Scratch Execution Harness")(
      test("Do Something") {
        for {
          // rs  <- ZIO.succeed(ctx.run(probeSch.filter(_.nAbC2n == "asdf")))
          rs1 <-
            inline def pPS(inline ps: P => Int): EntryRecord[P] => Int = r => ps(r.payload)
            inline def pS                                              = (p: P) => p.a
            ZIO.succeed(
              ctx.run(
                query[EntryRecord[P]].sortBy(er =>
                  (EntryRecord.liftSort[P](p => Tuple1(p.a))(er), EntryRecord.defaultSort[P](er))
                )(
                  EntryRecord.sortDir(Ord.ascNullsLast)
                )
              )
            )
        } yield
          println(s"#### FIRST: ${rs1.string(true)}")
          assertCompletes
      },
      test("Try Dynamic OrderBy") {
        for {
          rs1 <-
            ZIO.succeed(
              ctx.run(quote(sql"${query[EntryRecord[P]]} ORDER BY x.x ASC, x.aa DESC".as[Query[EntryRecord[P]]]))
            )
        } yield
          println(s"#### SECOND: ${rs1.string(true)}")
          assertCompletes
      }
    ) @@ sequential
