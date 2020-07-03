package forex.services.rates

import scala.concurrent.duration.FiniteDuration

import cats.Applicative
import cats.effect.Sync
import interpreters._
import forex.services.ratesBoard.{ Algebra => RatesBoard }

object Interpreters {
  def dummy[F[_]: Applicative](): Algebra[F] = new OneFrameDummy[F]()
  def live[F[_]: Sync](board: RatesBoard[F], expiration: FiniteDuration, dateProvider: DateProvider) =
    new OneFrameLive[F](board, expiration, dateProvider)
}
