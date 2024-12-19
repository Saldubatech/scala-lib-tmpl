package com.saldubatech.infrastructure.storage.memory

import com.saldubatech.infrastructure.storage.DataRecord
import com.saldubatech.infrastructure.storage.rdbms.quill.DynamicPredicate
import com.saldubatech.lang.Id

final case class ItemEvo(override val rId: Id, name: String, price: BigDecimal) extends DataRecord

class InMemoryItemService extends Domain.Service[ItemEvo](collection.mutable.Map.empty):

end InMemoryItemService // class
