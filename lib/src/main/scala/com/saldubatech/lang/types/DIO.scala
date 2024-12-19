package com.saldubatech.lang.types

import zio.ZIO

type RDIO[R, RS] = ZIO[R, AppResult.Error, RS]

type DIO[RS] = RDIO[Any, RS]
