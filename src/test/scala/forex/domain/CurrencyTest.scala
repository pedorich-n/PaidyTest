package forex.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class CurrencyTest extends AnyWordSpecLike with Matchers {

  "Currency" should {
    "return all pairs" in {
      Currency.allPairs.size shouldBe 72
      Currency.allPairs.exists { case (left, right) => left == right } shouldBe false
    }
  }
}
