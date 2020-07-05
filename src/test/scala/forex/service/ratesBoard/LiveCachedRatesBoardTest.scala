package forex.service.ratesBoard

import scala.concurrent.duration.FiniteDuration

import java.time.{ OffsetDateTime, ZoneOffset }
import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

import cats.effect.concurrent.Ref
import cats.effect.{ IO, Sync }
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.OneFrameService
import forex.services.oneframe.errors
import forex.services.oneframe.errors.Error.{ OneFrameTimeoutError, OneFrameUnknownError, OneFrameUnreachableError }
import forex.services.ratesBoard.interpreters.LiveCachedRatesBoard
import forex.tools.AsyncIOSpec

class LiveCachedRatesBoardTest extends AsyncWordSpecLike with Matchers with AsyncIOSpec {

  class StatefulOneFrameService[F[_]: Sync](values: IndexedSeq[Either[errors.Error, List[Rate]]])
      extends OneFrameService[F] {
    private val counter: AtomicInteger = new AtomicInteger(0)

    override def getMany(pairs: Seq[Rate.Pair]): F[Either[errors.Error, List[Rate]]] =
      Sync[F].delay(values(counter.getAndIncrement()))
  }

  "LiveCachedRatesBoard" should {
    "update values in the background via Ref and Stream" in {
      val rate: Rate = Rate(
        Rate.Pair(Currency.USD, Currency.AUD),
        Price(BigDecimal(0.5D)),
        Timestamp(OffsetDateTime.of(2020, 7, 5, 11, 0, 0, 0, ZoneOffset.UTC))
      )

      val states: Vector[Either[errors.Error, List[Rate]]] =
        Vector(Right(List.empty), Right(List.empty), Right(List(rate)))

      val cache: Ref[IO, Map[Rate.Pair, Rate]] = Ref.unsafe(Map.empty)
      val service: LiveCachedRatesBoard[IO] =
        LiveCachedRatesBoard(new StatefulOneFrameService(states), cache, FiniteDuration(2, "seconds"))

      service.updaterStream().take(3).compile.drain.flatMap { _ =>
        service.getRates.asserting { rates: Map[Rate.Pair, Rate] =>
          rates should contain theSameElementsAs Map(Rate.Pair(Currency.USD, Currency.AUD) -> rate)
        }
      }
    }

    "suppress errors" in {
      val rate: Rate = Rate(
        Rate.Pair(Currency.USD, Currency.AUD),
        Price(BigDecimal(0.5D)),
        Timestamp(OffsetDateTime.of(2020, 7, 5, 11, 0, 0, 0, ZoneOffset.UTC))
      )

      val states: Vector[Either[errors.Error, List[Rate]]] =
        Vector(
          Left(OneFrameUnknownError("Unknown!")),
          Left(OneFrameUnreachableError("Connection refused!")),
          Left(OneFrameTimeoutError),
          Right(List(rate))
        )

      val cache: Ref[IO, Map[Rate.Pair, Rate]] = Ref.unsafe(Map.empty)
      val service: LiveCachedRatesBoard[IO] =
        LiveCachedRatesBoard(new StatefulOneFrameService(states), cache, FiniteDuration(2, "seconds"))

      service.updaterStream().take(4).compile.drain.flatMap { _ =>
        service.getRates.asserting { rates: Map[Rate.Pair, Rate] =>
          rates should contain theSameElementsAs Map(Rate.Pair(Currency.USD, Currency.AUD) -> rate)
        }
      }
    }

  }
}
