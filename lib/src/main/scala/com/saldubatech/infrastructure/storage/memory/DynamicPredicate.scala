package com.saldubatech.infrastructure.storage.memory

import com.saldubatech.infrastructure.storage.JournaledDomain.EntryRecord
import com.saldubatech.infrastructure.storage.{ParametricDynamicPredicate, Payload}

object DynamicPredicate:
  type NOOP[H] = H

end DynamicPredicate // object

class DynamicPredicate[H] private[memory] (t: String)
    extends ParametricDynamicPredicate[H, DynamicPredicate.NOOP, DynamicPredicate.NOOP]:

  import DynamicPredicate.*

  inline def apply(inline q: NOOP[H]): NOOP[NOOP[H]] = ???
  inline def count(inline q: NOOP[H]): NOOP[Long]    = ???

  inline def journaled(inline q: NOOP[EntryRecord[H & Payload]]): NOOP[NOOP[EntryRecord[H & Payload]]] = ???

end DynamicPredicate
