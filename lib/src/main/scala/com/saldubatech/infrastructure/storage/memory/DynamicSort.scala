package com.saldubatech.infrastructure.storage.memory

import com.saldubatech.infrastructure.storage.JournaledDomain.EntryRecord
import com.saldubatech.infrastructure.storage.{ParametricDynamicSort, Payload}

class DynamicSort[H] private[memory] (t: String) extends ParametricDynamicSort[H, DynamicPredicate.NOOP, DynamicPredicate.NOOP]:

  import DynamicPredicate.NOOP

  inline def apply(inline q: NOOP[H]): NOOP[NOOP[H]] = ???

  inline def journaled(inline q: NOOP[EntryRecord[H & Payload]]): NOOP[NOOP[EntryRecord[H & Payload]]] = ???

end DynamicSort // class
