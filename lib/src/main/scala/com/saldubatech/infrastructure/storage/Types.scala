package com.saldubatech.infrastructure.storage

import com.saldubatech.lang.types.AppResult
import zio.ZIO

type DIO[RS] = ZIO[Any, AppResult.Error, RS]
