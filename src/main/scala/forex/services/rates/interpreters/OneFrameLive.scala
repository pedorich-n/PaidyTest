package forex.services.rates.interpreters

import scala.concurrent.duration.FiniteDuration

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

import cats.effect.Sync
import cats.syntax.functor._
import forex.domain.Rate
import forex.services.rates.errors.Error.LookupFailed
import forex.services.rates.{Algebra, DateProvider, errors}
import forex.services.ratesBoard.{Algebra => RatesBoard}

class OneFrameLive[F[_]: Sync](board: RatesBoard[F], expiration: FiniteDuration, dateProvider: DateProvider)
    extends Algebra[F] {

  override def get(request: Rate.Pair): F[Either[errors.Error, Rate]] = {
    def isOld(dateTime: OffsetDateTime): Boolean =
      dateTime.toEpochSecond > dateProvider.getNow.plus(expiration.toMillis, ChronoUnit.MILLIS).toEpochSecond

    board.getRates.map { rates: Map[Rate.Pair, Rate] =>
      rates
        .get(request)
        .toRight(LookupFailed("Rates pair is missing!"))
        .flatMap { rate: Rate =>
          if (isOld(rate.timestamp.value)) Left(LookupFailed("Rate is too old!"))
          else Right(rate)
        }
    }
  }
}
