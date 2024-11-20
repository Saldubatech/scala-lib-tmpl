package com.saldubatech.infrastructure.storage.memory

import com.saldubatech.infrastructure.storage.JournaledDomain.EntryRecord
import com.saldubatech.lang.Id

final case class SamplePayload(name: String, price: Double)

class InMemorySampleJournal
    extends LinearJournal.Service[SamplePayload](
      "SampleInMemoryJournal",
      collection.mutable.Map.empty[Id, Map[Id, EntryRecord[SamplePayload]]]
    ):

end InMemorySampleJournal
