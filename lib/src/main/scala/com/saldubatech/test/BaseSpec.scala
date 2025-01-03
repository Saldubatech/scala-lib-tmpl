package com.saldubatech.test

import com.saldubatech.util.LogEnabled
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{AnyWordSpec, AnyWordSpecLike}
import org.scalatest.{BeforeAndAfterAll, EitherValues}

import scala.collection.mutable

object BaseSpec

trait BaseSpec extends AnyWordSpec with Matchers with AnyWordSpecLike with BeforeAndAfterAll with EitherValues with LogEnabled:

  val name: String = this.getClass.getName + "_Spec"

  def unsupported: Nothing = throw new UnsupportedOperationException()

end BaseSpec // trait
