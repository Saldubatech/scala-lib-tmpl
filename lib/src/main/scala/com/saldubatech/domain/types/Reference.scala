package com.saldubatech.domain.types

import com.saldubatech.infrastructure.services.ServiceLocalAddress
import com.saldubatech.lang.Id
import com.saldubatech.lang.types.meta.MetaType

case class Reference(home: ServiceLocalAddress, localId: Id) derives MetaType

object Reference:

end Reference // object
