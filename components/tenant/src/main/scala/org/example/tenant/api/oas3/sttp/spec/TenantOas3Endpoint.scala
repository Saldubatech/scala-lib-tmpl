package org.example.tenant.api.oas3.sttp.spec

import com.saldubatech.infrastructure.network.oas3.entity.{EntityResult, PageResult}
import com.saldubatech.lang.types.meta.MetaType
import com.saldubatech.lang.query.*
import com.saldubatech.lang.Id
import sttp.model.QueryParams
import sttp.tapir.ztapir.*
import sttp.tapir.{Codec, CodecFormat, EndpointIO, EndpointInput, Schema as TSchema, SchemaType}
import sttp.tapir.json.circe.jsonBody
import io.circe.generic.semiauto.*
import sttp.tapir.generic.auto.*

object Tapir:

//  val orderDT = summon[MetaType[Order]]
//
//  given TSchema[ValueType.VALUE]  = TSchema.derivedUnion[ValueType.VALUE]
//  given TSchema[Projectable.Step] = TSchema.derived[Projectable.Step]
//  given TSchema[Projection]       = TSchema.derived[Projection]
//  given TSchema[OrderDirection]   = TSchema.derived[OrderDirection]
//
//  given TSchema[Page]      = TSchema.derived[Page]
//  given TSchema[OrderTerm] = TSchema.derived[OrderTerm]
//  given TSchema[Order]     = TSchema.derived[Order]
//
//  implicit def itvSc[T: TSchema]: TSchema[Interval[T]] = TSchema.derived[Interval[T]]
//  given TSchema[Filter.Compare]                        = TSchema.derived[Filter.Compare]
//  given TSchema[Filter.Range]                          = TSchema.derived[Filter.Range]
//
//  given cmpsSc: TSchema[Filter.Composite] = TSchema.derived[Filter.Composite]
//  given TSchema[Filter.Literal]           = TSchema.derived[Filter.Literal]
//
//  given TSchema[Filter] = TSchema.derived[Filter]
//
//  given TSchema[Query] = TSchema.derived[Query]
//
//  given TSchema[Home] = TSchema.derived[Home]
//
//  given TSchema[ServiceEndpoint] = TSchema.derived[ServiceEndpoint]
//
//  given TSchema[Entity.Id] = TSchema.derived[Entity.Id]
//
//  implicit def entitySch[E: TSchema]: TSchema[Entity[E]] = TSchema.derived[Entity[E]]
//
//  implicit def pr[E: TSchema]: TSchema[PageResult[E]] = TSchema.derived[PageResult[E]]

  object QueryParameters:

    // Entity Id. To be changed to encode a `Reference`
    val id = query[Id]("id")
    // Encoded Page Definition. See Page type
    val pageId = query[String]("pageId")

  end QueryParameters

  object Inputs:

    // val query = jsonBody[Query]

  end Inputs

  object Outputs:

//    def entitySch[E: TSchema] = jsonBody[Entity[E]]

//    def pageResut[RS: TSchema](using DomainType[EntityOperations.PageResult[RS]]) = jsonBody[EntityOperations.PageResult[RS]]

  end Outputs

  object IOs:
  end IOs

end Tapir

object TenantOas3Endpoint:

//  val version: String = "1-0-0-SNAPSHOT"
//  val name: String    = "tenants"

// val query = endpoint.name(s"query$name").in(version / name / "query").post.in(Tapir.Inputs.query).out(Tapir.Outputs.pageResut)

end TenantOas3Endpoint
