package forex.services.ratesBoard

import scala.concurrent.duration.FiniteDuration

import cats.effect.concurrent.Ref
import cats.effect.{ Async, Timer }
import forex.domain.Rate
import forex.services.OneFrameService
import forex.services.ratesBoard.interpreters.LiveCachedRatesBoard

object Interpreters {

  def live[F[_]: Async: Timer](oneFrame: OneFrameService[F],
                               cache: Ref[F, Map[Rate.Pair, Rate]],
                               sleepDuration: FiniteDuration): LiveCachedRatesBoard[F] =
    LiveCachedRatesBoard(oneFrame, cache, sleepDuration)

}
