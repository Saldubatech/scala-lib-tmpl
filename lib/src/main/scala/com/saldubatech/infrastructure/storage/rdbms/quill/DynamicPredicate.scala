package com.saldubatech.infrastructure.storage.rdbms.quill

import com.saldubatech.infrastructure.storage.{
  JournalEntry,
  ParametricDynamicPredicate,
  ParametricDynamicPredicateFactory,
  Payload
}
import com.saldubatech.infrastructure.storage.JournaledDomain.EntryRecord
import com.saldubatech.lang.query.{Filter, Projectable, Projection, ValueType}
import com.saldubatech.lang.query.Filter.{Compare, Range}
import com.saldubatech.lang.types.*
import io.getquill.*
import zio.ZIO

object DynamicPredicate:

end DynamicPredicate

class DynamicPredicate[H] private[quill] (t: String) extends ParametricDynamicPredicate[H, Query, Quoted]:

  override def toString = t

  inline def apply(inline q: Query[H]): Quoted[Query[H]] = quote(q.filter(h => sql"#$t".asCondition))
  inline def count(inline q: Query[H]): Quoted[Long]     = quote(q.filter(h => sql"#$t".asCondition).size)

  inline def journaled(inline q: Query[EntryRecord[H & Payload]]): Quoted[Query[EntryRecord[H & Payload]]] =
    quote(q.filter(h => sql"#$t".pure.asCondition))

end DynamicPredicate

class DynamicPredicateFactory[H <: Product: Projectable]
    extends ParametricDynamicPredicateFactory[H, Query, Quoted, DynamicPredicate[H]]:

  override def apply(f: Filter): AppResult[DynamicPredicate[H]] = toTerm(f).map(str => new DynamicPredicate[H](str))

  override def journaled(f: Filter): AppResult[DynamicPredicate[H]] = toTerm(f).map(str => new DynamicPredicate(str))

  override def eval(f: Filter)(q: String => DIO[Iterable[H]]): DIO[Iterable[H]] = toTerm(f).fold(err => ZIO.fail(err), q(_))

  // TBD: This could use a Schema[H] or Schema[JournalEntry[H]] to validate that the path is correct.
  private def toTerm(f: Filter): AppResult[String] = toTermUnsafe(f)

  private def toTermUnsafe(f: Filter): AppResult[String] =
    f match
      case Filter.Literal.TRUE  => AppResult.Success("TRUE")
      case Filter.Literal.FALSE => AppResult.Success("FALSE")
      case Filter.And(clauses)  => clauses.map(c => toTermUnsafe(c)).collectAll.map(_.mkString("(", " AND ", ")"))
      case Filter.Or(clauses)   => clauses.map(c => toTermUnsafe(c)).collectAll.map(_.mkString("(", " OR ", ")"))
      case Filter.Not(c)        => toTermUnsafe(c).map(cStr => s"NOT $cStr")
      case t: Filter.Compare    => compare(t)
      case rg: Filter.Range     => intervalCheck(rg)

  private def compare(cmp: Filter.Compare): AppResult[String] =
    DynamicProjection.project(cmp.locator).map { l =>
      cmp match
        case Compare.Eq(_, reference) => s"($l = ${value(reference)})"
        case Compare.Ne(_, reference) => s"($l <> ${value(reference)})"
        case Compare.Gt(_, reference) => s"($l > ${value(reference)})"
        case Compare.Ge(_, reference) => s"($l >= ${value(reference)})"
        case Compare.Lt(_, reference) => s"($l < ${value(reference)})"
        case Compare.Le(_, reference) => s"($l <= ${value(reference)})"
    }

  private def intervalCheck(rg: Filter.Range): AppResult[String] =
    rg match
      case Range.Between(l, reference) =>
        DynamicProjection.project(l).map(lStr => s"(h.$lStr BETWEEN ${value(reference.min)} AND ${value(reference.max)})")

  private def value[V <: ValueType.VALUE](v: V): String =
    v match
      case s: String  => s"'$s'"
      case b: Boolean => if b then "TRUE" else "FALSE"
      case i: Int     => i.toString
      case l: Long    => l.toString
      case f: Float   => f.toString
      case d: Double  => d.toString
      // case bd: BigDecimal => bd.doubleValue.toString

end DynamicPredicateFactory // object
