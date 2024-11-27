package org.example.api.tenant

import org.example.api.lib.bindings.ServiceEndpoint
import org.example.api.lib.requestresponse.Listener
import org.example.api.tenant.domain.ChangeNotification

class ChangeListener(location: ServiceEndpoint)
    extends Listener[ChangeNotification.Change](location)(using ChangeNotification.schema)
