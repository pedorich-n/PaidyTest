package forex.services.ratesBoard.interpreters

import scala.concurrent.duration.FiniteDuration

import cats.effect.concurrent.Ref
import cats.effect.{ Async, Timer }
import forex.domain.{ Currency, Rate }
import forex.services.oneframe.{ Algebra => OneFrameAlgebra }
import forex.services.ratesBoard.Algebra
import fs2.Stream
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.applicativeError._
import io.chrisdavenport.log4cats.{ Logger, SelfAwareStructuredLogger }
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

class LiveCachedRatesBoard[F[_]: Async: Timer: Logger](oneFrame: OneFrameAlgebra[F],
                                                       cache: Ref[F, Map[Rate.Pair, Rate]],
                                                       sleepDuration: FiniteDuration)
    extends Algebra[F] {

  private val getLogger: F[SelfAwareStructuredLogger[F]] = Slf4jLogger.create[F]

  override def getRates: F[Map[Rate.Pair, Rate]] = cache.get

  private val allPairs: List[Rate.Pair] = Currency.allPairs.map(Rate.Pair.tupled)

  def backgroundStream(): Stream[F, Unit] = {
    def getAndSetRates(): F[Unit] = {
      val process: F[Unit] = oneFrame
        .getMany(allPairs)
        .flatMap(Async[F].fromEither)
        .map((rates: List[Rate]) => rates.map((rate: Rate) => rate.pair -> rate).toMap)
        .flatMap((newMap: Map[Rate.Pair, Rate]) => cache.set(newMap))

      process.handleErrorWith { t: Throwable =>
        getLogger.flatMap(_.error(t)("An exception occurred in RatesBoard!"))
      }
    }

    Stream.eval(getAndSetRates()) >> Stream.awakeEvery[F](sleepDuration) >> Stream.eval(getAndSetRates())
  }
}

object LiveCachedRatesBoard {
  def apply[F[_]: Async: Timer: Logger](oneFrame: OneFrameAlgebra[F],
                                        sleepDuration: FiniteDuration): F[LiveCachedRatesBoard[F]] =
    Ref.of(Map.empty[Rate.Pair, Rate]).map(new LiveCachedRatesBoard(oneFrame, _, sleepDuration))
}
