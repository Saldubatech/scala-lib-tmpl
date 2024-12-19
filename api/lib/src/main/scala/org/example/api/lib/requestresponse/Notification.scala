package org.example.api.lib.requestresponse

import com.saldubatech.lang.types.datetime.Epoch
import com.saldubatech.lang.Id
import zio.schema.DeriveSchema

object Notification:

  sealed trait Ack:

    val notificationId: Id
    val at: Epoch

  end Ack // trait
  object Ack:

    val schema = DeriveSchema.gen[Ack]
    case class OK(override val notificationId: Id, override val at: Epoch)                  extends Ack
    case class NOK(override val notificationId: Id, override val at: Epoch, reason: String) extends Ack

  end Ack        // object
end Notification // object

trait Notification:

  val id: Id
  val at: Epoch

end Notification // trait
