package com.saldubatech.infrastructure.storage

import com.saldubatech.infrastructure.storage.JournaledDomain.EntryRecord
import com.saldubatech.lang.query.{Filter, Projectable}
import com.saldubatech.lang.types.{AppResult, DIO}

trait ParametricDynamicPredicate[H, Q[_], QL[_]]:

  inline def apply(inline q: Q[H]): QL[Q[H]]
  inline def count(inline q: Q[H]): QL[Long]
  inline def journaled(inline q: Q[EntryRecord[H & Payload]]): QL[Q[EntryRecord[H & Payload]]]

end ParametricDynamicPredicate

trait ParametricDynamicPredicateFactory[H <: Product: Projectable, Q[_], QL[_], DP <: ParametricDynamicPredicate[H, Q, QL]]:

  extension (q: Q[H])

    inline def apply(d: ParametricDynamicPredicate[H, Q, QL]): QL[Q[H]] = d(q)
    inline def count(d: ParametricDynamicPredicate[H, Q, QL]): QL[Long] = d.count(q)

  def apply(f: Filter): AppResult[DP]
  def journaled(f: Filter): AppResult[DP]
  def eval(f: Filter)(q: String => DIO[Iterable[H]]): DIO[Iterable[H]]

end ParametricDynamicPredicateFactory // trait
