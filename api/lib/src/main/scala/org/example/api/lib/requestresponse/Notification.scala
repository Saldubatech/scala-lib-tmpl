package org.example.api.lib.requestresponse

import com.saldubatech.lang.types.datetime.Epoch
import org.example.api.lib.types.UUID
import zio.schema.DeriveSchema

object Notification:

  sealed trait Ack:

    val notificationId: UUID
    val at: Epoch

  end Ack // trait
  object Ack:

    val schema = DeriveSchema.gen[Ack]
    case class OK(override val notificationId: UUID, override val at: Epoch)                  extends Ack
    case class NOK(override val notificationId: UUID, override val at: Epoch, reason: String) extends Ack

  end Ack        // object
end Notification // object

trait Notification:

  val id: UUID
  val at: Epoch

end Notification // trait
