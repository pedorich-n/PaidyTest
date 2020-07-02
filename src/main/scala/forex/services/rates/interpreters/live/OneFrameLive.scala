package forex.services.rates.interpreters.live

import scala.concurrent.duration.FiniteDuration

import java.util.concurrent.TimeUnit

import cats.effect.concurrent.Ref
import cats.effect.{Async, Timer}
import cats.syntax.functor._
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.rates.{Algebra, errors}
import fs2.Stream

class OneFrameLive[F[_]: Async: Timer](rates: Ref[F, Map[Rate.Pair, Rate]]) extends Algebra[F] {
  private val sleepDuration: FiniteDuration = FiniteDuration(20, TimeUnit.SECONDS) // 4.5 minutes //TODO: config

  def awakeEveryDuration(duration: FiniteDuration): Stream[F, Unit] =
    Stream.sleep(duration).repeat

  override def get(request: Rate.Pair): F[Either[errors.Error, Rate]] =
    rates.get.map(_.get(request).toRight(errors.Error.OneFrameLookupFailed("oops"))) //TODO: error if timestamp is old

  def lookupService(): Stream[F, Unit] = {
    def setRates(): Stream[F, Unit] =
      for {
        newRates <- Stream.eval(getRates)
        _ = println("setting rates")
        _ <- Stream.eval(rates.set(newRates))
      } yield ()

    setRates() >> awakeEveryDuration(sleepDuration) >> setRates()
  }

  def getRates: F[Map[Rate.Pair, Rate]] =
    Async[F].pure(
      Map(
        Rate.Pair(Currency.USD, Currency.SGD) -> Rate(
          Rate.Pair(Currency.USD, Currency.SGD),
          Price(BigDecimal.valueOf(2D)),
          Timestamp.now
        )
      )
    )
}

