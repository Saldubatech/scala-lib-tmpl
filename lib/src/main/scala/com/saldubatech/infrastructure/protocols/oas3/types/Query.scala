package com.saldubatech.infrastructure.protocols.oas3.types

case class Query(
    page: Page = Page.default,
    filter: Option[Filter] = None,
    order: Option[Order] = None)
