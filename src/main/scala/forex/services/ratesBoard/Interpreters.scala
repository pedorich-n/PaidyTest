package forex.services.ratesBoard

import scala.concurrent.duration.FiniteDuration

import cats.effect.{ Async, Timer }
import cats.effect.concurrent.Ref
import forex.domain.Rate
import forex.services.OneFrameService
import forex.services.ratesBoard.interpreters.LiveCachedRatesBoard
import io.chrisdavenport.log4cats.Logger

object Interpreters {

  def live[F[_]: Async: Timer: Logger](oneFrame: OneFrameService[F],
                                       cache: Ref[F, Map[Rate.Pair, Rate]],
                                       sleepDuration: FiniteDuration): LiveCachedRatesBoard[F] =
    LiveCachedRatesBoard(oneFrame, cache, sleepDuration)

}
