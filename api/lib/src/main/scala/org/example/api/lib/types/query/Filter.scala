package org.example.api.lib.types.query

sealed trait Filter

object Filter:

  case class And(clauses: List[Filter]) extends Filter
  case class Or(clauses: List[Filter])  extends Filter
  case class Not(filter: Filter)        extends Filter

  sealed trait Value
  object Value:

    case class IntValue(v: Int)    extends Value
    case class BoolValue(v: Int)   extends Value
    case class FloatValue(v: Int)  extends Value
    case class StringValue(v: Int) extends Value

  end Value // object

  sealed trait Interval
  object Interval:

    case class IntInterval(min: Int, max: Int, minClosed: Boolean = true, maxClosed: Boolean = false)          extends Interval
    case class BoolInterval(min: Boolean, max: Boolean, minClosed: Boolean = true, maxClosed: Boolean = false) extends Interval

    case class FloatInterval(min: BigDecimal, max: BigDecimal, minClosed: Boolean = true, maxClosed: Boolean = false)
        extends Interval

    case class StringInterval(min: String, max: String, minClosed: Boolean = true, maxClosed: Boolean = false) extends Interval

  end Interval // object
  case class Eq(locator: Projection, value: Value) extends Filter

  case class Ne(locator: Projection, value: Value) extends Filter

  case class Gt(locator: Projection, value: Value) extends Filter
  case class Ge(locator: Projection, value: Value) extends Filter
  case class Lt(locator: Projection, value: Value) extends Filter
  case class Le(locator: Projection, value: Value) extends Filter

  case class Between(locator: Projection, interval: Interval) extends Filter
  case class Match(locator: Projection, value: String)        extends Filter

end Filter
