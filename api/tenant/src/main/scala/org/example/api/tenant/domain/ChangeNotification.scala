package org.example.api.tenant.domain

import com.saldubatech.lang.types.datetime.Epoch
import org.example.api.lib.requestresponse.Notification
import org.example.api.lib.types.UUID
import org.example.api.types.Entity
import zio.schema.DeriveSchema

object ChangeNotification:

  val schema = DeriveSchema.gen[Change]

  sealed trait Change extends Notification:
    val tenant: TenantSummary

  case class Create(override val id: UUID, override val at: Epoch, tenant: TenantSummary) extends Change
  case class Update(override val id: UUID, override val at: Epoch, tenant: TenantSummary) extends Change
  case class Delete(override val id: UUID, override val at: Epoch, tenant: TenantSummary) extends Change

end ChangeNotification // object
