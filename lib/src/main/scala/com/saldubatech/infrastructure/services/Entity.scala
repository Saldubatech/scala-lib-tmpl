package com.saldubatech.infrastructure.services

import com.saldubatech.domain.types.Reference

trait Entity:
  val locator: Reference
