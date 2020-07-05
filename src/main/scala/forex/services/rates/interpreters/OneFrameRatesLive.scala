package forex.services.rates.interpreters

import scala.concurrent.duration.FiniteDuration

import java.time.{ OffsetDateTime, ZoneOffset }
import java.time.temporal.ChronoUnit

import cats.effect.Sync
import cats.syntax.functor._
import forex.domain.Rate
import forex.services.RatesBoardService
import forex.services.rates.errors.Error.LookupFailed
import forex.services.rates.{ errors, Algebra, DateTimeProvider }

/**
 * OneFrame live implementation of Rates Service that relies on RatesBoard Service
 * @param board RatesBoard instance
 * @param expiration When to consider a rate expired
 * @param dateProvider DateTimeProvider instance
 * @tparam F Effect type
 */
class OneFrameRatesLive[F[_]: Sync](board: RatesBoardService[F],
                                    expiration: FiniteDuration,
                                    dateProvider: DateTimeProvider)
    extends Algebra[F] {

  override def get(request: Rate.Pair): F[Either[errors.Error, Rate]] = {
    def isOld(dateTime: OffsetDateTime): Boolean =
      dateProvider.getNowUTC
        .isAfter(dateTime.atZoneSameInstant(ZoneOffset.UTC).plus(expiration.toMillis, ChronoUnit.MILLIS))

    board.getRates.map { rates: Map[Rate.Pair, Rate] =>
      rates
        .get(request)
        .toRight(LookupFailed("Rate is missing!"))
        .flatMap { rate: Rate =>
          if (isOld(rate.timestamp.value)) Left(LookupFailed("Rate has expired!"))
          else Right(rate)
        }
    }
  }
}
