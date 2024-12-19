package org.example.tenant.api.oas3.zio.spec

import com.saldubatech.infrastructure.network.Network.ServiceLocator
import org.example.api.lib.requestresponse.Listener
import org.example.tenant.api.oas3.zio.domain.ChangeNotification

class ChangeListener(location: ServiceLocator)
    extends Listener[ChangeNotification.Change](location)(using ChangeNotification.schema)
