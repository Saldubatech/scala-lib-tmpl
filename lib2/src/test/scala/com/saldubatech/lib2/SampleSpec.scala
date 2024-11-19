package com.saldubatech.lib2

import com.saldubatech.test.BaseSpec

class SampleSpec extends BaseSpec {
  "A Number" when {
    "It is initialized to zero" must {
      "equal zero (Long)" in {
        val z = 0L
        z shouldBe 0L
      }
    }
    "is initialized with something else" must {
      "be a positive number" in {
        val p = 33L
        p should be > 0L
      }
    }
  }
}
