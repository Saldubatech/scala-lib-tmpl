package com.saldubatech.infrastructure.storage

import com.saldubatech.lang.Id

object Record:
end Record

trait DataRecord extends Product with Serializable:
  val rId: Id

end DataRecord // trait
