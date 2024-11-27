package org.example.api.types

case class Address(
    firstLine: String,
    secondLine: Option[String],
    city: String,
    region: String,
    postalCode: String,
    country: String)
    extends Value
