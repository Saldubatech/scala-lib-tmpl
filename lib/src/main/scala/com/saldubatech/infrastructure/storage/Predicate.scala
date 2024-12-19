package com.saldubatech.infrastructure.storage

import com.saldubatech.lang.query.{Filter, Projection}

import scala.reflect.Typeable

type Term[H]       = H => Boolean
type PRJ[H, ATTR]  = H => ATTR
type SORT[H]       = H => Tuple
type SORTDIR[O[_]] = O[Tuple]

object Predicate:

  inline def True[T]: Term[T]                                      = _ => true
  inline def False[T]: Term[T]                                     = _ => false
  inline def and[T](inline l: Term[T], inline r: Term[T]): Term[T] = (t: T) => l(t) && r(t)
  inline def or[T](inline l: Term[T], inline r: Term[T]): Term[T]  = (t: T) => l(t) || r(t)
  inline def not[T](inline c: Term[T]): Term[T]                    = (t: T) => !c(t)

end Predicate // object

object Projection:
  inline def project[V, ATTR](inline p: PRJ[V, ATTR], inline t: Term[ATTR]): Term[V] = r => t(p(r))

end Projection // object

object Sort:

end Sort // object
