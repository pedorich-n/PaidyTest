package forex.service.rates

import java.time.{ Instant, ZonedDateTime }

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import forex.services.rates.interpreters.DefaultDateTimeProvider

class DefaultDateTimeProviderTest extends AnyWordSpecLike with Matchers {

  val provider: DefaultDateTimeProvider = new DefaultDateTimeProvider()

  "DefaultDateTimeProvider" should {
    "return valid UTC time" in {
      (provider.getNowUTC.toEpochSecond - Instant.now().getEpochSecond) < 5 shouldBe true
    }

    "return valid local time" in {
      val provided: ZonedDateTime = provider.getNowLocal
      val expected: ZonedDateTime = ZonedDateTime.now()

      provided.getZone shouldBe expected.getZone
      (provided.toEpochSecond - expected.toEpochSecond) < 5 shouldBe true
    }
  }
}
