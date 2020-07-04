package forex.service.rates

import scala.concurrent.duration.FiniteDuration

import java.time.{ OffsetDateTime, ZoneOffset, ZonedDateTime }

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

import cats.effect.{ IO, Sync }
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.RatesBoardService
import forex.services.rates.DateTimeProvider
import forex.services.rates.errors.Error.LookupFailed
import forex.services.rates.interpreters.OneFrameRatesLive
import forex.tools.AsyncIOSpec

class OneFrameRatesLiveTest extends AsyncWordSpecLike with Matchers with AsyncIOSpec {

  val mockedDateTimeProvider: DateTimeProvider = new DateTimeProvider {
    override def getNowLocal: ZonedDateTime = ZonedDateTime.of(2020, 7, 4, 15, 10, 0, 0, ZoneOffset.ofHours(2))

    override def getNowUTC: ZonedDateTime = ZonedDateTime.of(2020, 7, 4, 13, 10, 0, 0, ZoneOffset.UTC)
  }

  class MockedRatesBoardService[F[_]: Sync](result: Map[Rate.Pair, Rate]) extends RatesBoardService[F] {
    override def getRates: F[Map[Rate.Pair, Rate]] = Sync[F].pure(result)
  }

  "OneFrameRatesLive" should {
    "return result" in {
      val data = Map(
        Rate.Pair(Currency.AUD, Currency.CAD) ->
          Rate(
            Rate.Pair(Currency.AUD, Currency.CAD),
            Price(BigDecimal("0.480309880869541285")),
            Timestamp(OffsetDateTime.of(2020, 7, 4, 15, 8, 0, 0, ZoneOffset.UTC))
          )
      )
      val service = new OneFrameRatesLive[IO](
        new MockedRatesBoardService(data),
        FiniteDuration(5, "minutes"),
        mockedDateTimeProvider
      )

      service
        .get(Rate.Pair(Currency.AUD, Currency.CAD))
        .asserting {
          _ shouldBe Right(
            Rate(
              Rate.Pair(Currency.AUD, Currency.CAD),
              Price(BigDecimal("0.480309880869541285")),
              Timestamp(OffsetDateTime.of(2020, 7, 4, 15, 8, 0, 0, ZoneOffset.UTC))
            )
          )
        }
    }

    "return error if rate is not found" in {
      val service = new OneFrameRatesLive[IO](
        new MockedRatesBoardService(Map.empty),
        FiniteDuration(5, "minutes"),
        mockedDateTimeProvider
      )

      service.get(Rate.Pair(Currency.AUD, Currency.CAD)).asserting(_ shouldBe Left(LookupFailed("Rate is missing!")))
    }

    "return error if rate is stale" in {
      val data = Map(
        Rate.Pair(Currency.AUD, Currency.CAD) ->
          Rate(
            Rate.Pair(Currency.AUD, Currency.CAD),
            Price(BigDecimal("0.480309880869541285")),
            Timestamp(OffsetDateTime.of(2020, 7, 4, 13, 4, 0, 0, ZoneOffset.UTC))
          )
      )

      val service = new OneFrameRatesLive[IO](
        new MockedRatesBoardService(data),
        FiniteDuration(5, "minutes"),
        mockedDateTimeProvider
      )

      service.get(Rate.Pair(Currency.AUD, Currency.CAD)).asserting(_ shouldBe Left(LookupFailed("Rate has expired!")))
    }

  }
}
