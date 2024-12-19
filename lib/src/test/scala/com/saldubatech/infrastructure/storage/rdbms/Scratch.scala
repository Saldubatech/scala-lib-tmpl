package com.saldubatech.infrastructure.storage.rdbms

import io.getquill.{querySchema, SchemaMeta}
import io.getquill.context.QuoteMacro
import io.getquill.metaprog.Extractors.GenericSeq

import scala.quoted.{Expr, Quotes, Type}

object Scratch:

  inline def schemaMetaExt[T](inline entity: String, inline columns: (T => (Any, String))*): SchemaMeta[T] =
    ${ SchemaMetaMacroExt[T]('entity, 'columns) }

end Scratch

object SchemaMetaMacroExt {

  // inline def schemaMeta[T](inline entity: String, inline columns: (T => (Any, String))*): SchemaMeta[T] =
  // SchemaMeta(quote { querySchema[T](entity, columns: _*) }, "1234") // TODO Don't need to generate a UID here.It can be static.
  def apply[T](entity: Expr[String], columns: Expr[Seq[(T => (Any, String))]])(using Quotes, Type[T]): Expr[SchemaMeta[T]] = {
    val uuid = Expr(java.util.UUID.randomUUID().toString)
//    val exprs =
//      (columns match {
//        case GenericSeq(argsExprs) => argsExprs
//      }).toList
    val quote = QuoteMacro('{ querySchema[T]($entity, $columns*) })
    '{ SchemaMeta($quote, $uuid) }
  }

}
