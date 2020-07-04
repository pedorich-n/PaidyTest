package forex.services.rates

import scala.concurrent.duration.FiniteDuration

import cats.Applicative
import cats.effect.Sync
import forex.services.RatesBoardService
import interpreters._

object Interpreters {
  def dummy[F[_]: Applicative](): Algebra[F] = new OneFrameDummy[F]()
  def live[F[_]: Sync](board: RatesBoardService[F], expiration: FiniteDuration, dateProvider: DateProvider) =
    new OneFrameLive[F](board, expiration, dateProvider)
}
