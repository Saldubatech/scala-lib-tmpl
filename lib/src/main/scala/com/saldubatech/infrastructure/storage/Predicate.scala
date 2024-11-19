package com.saldubatech.infrastructure.storage

type Term[T]      = T => Boolean
type PRJ[T, ATTR] = T => ATTR

object Predicate:

  inline def and[T](inline l: Term[T], inline r: Term[T]): Term[T] = (t: T) => l(t) && r(t)
  inline def or[T](inline l: Term[T], inline r: Term[T]): Term[T]  = (t: T) => l(t) || r(t)
  inline def not[T](inline c: Term[T]): Term[T]                    = (t: T) => !c(t)

  inline def project[V, ATTR](inline p: PRJ[V, ATTR], inline t: Term[ATTR]): Term[V] = r => t(p(r))
