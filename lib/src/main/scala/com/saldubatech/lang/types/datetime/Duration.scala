package com.saldubatech.lang.types.datetime

type Duration = Long

object Duration:

  val zero: Duration = 0L

  def since(t: Epoch): Duration = Epoch.now - t

end Duration // object
