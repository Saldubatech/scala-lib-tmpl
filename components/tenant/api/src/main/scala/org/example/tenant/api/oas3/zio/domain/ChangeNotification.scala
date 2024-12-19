package org.example.tenant.api.oas3.zio.domain

import com.saldubatech.lang.types.datetime.Epoch
import com.saldubatech.lang.Id
import org.example.api.lib.requestresponse.Notification
import zio.schema.DeriveSchema

object ChangeNotification:

  val schema = DeriveSchema.gen[Change]

  sealed trait Change extends Notification:
    val tenant: TenantSummary

  case class Create(override val id: Id, override val at: Epoch, tenant: TenantSummary) extends Change
  case class Update(override val id: Id, override val at: Epoch, tenant: TenantSummary) extends Change
  case class Delete(override val id: Id, override val at: Epoch, tenant: TenantSummary) extends Change

end ChangeNotification // object
