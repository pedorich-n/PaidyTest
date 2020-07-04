package forex.service.oneframe

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import forex.services.oneframe.interpreters.StaticTokenProvider

class StaticTokenProviderTest extends AnyWordSpecLike with Matchers {

  "StaticTokenProvider" should {
    "echo the input" in {
      new StaticTokenProvider("example").getToken shouldBe "example"
    }
  }
}
