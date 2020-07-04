package forex.services.ratesBoard.interpreters

import scala.concurrent.duration.FiniteDuration

import cats.effect.concurrent.Ref
import cats.effect.{Async, Timer}
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import forex.domain.{Currency, Rate}
import forex.services.oneframe.errors.Error.{OneFrameTimeoutError, OneFrameUnknownError, OneFrameUnreachableError}
import forex.services.{OneFrameService, RatesBoardService}
import fs2.Stream
import io.chrisdavenport.log4cats.Logger

class LiveCachedRatesBoard[F[_]: Async: Timer: Logger](oneFrame: OneFrameService[F],
                                                       cache: Ref[F, Map[Rate.Pair, Rate]],
                                                       sleepDuration: FiniteDuration)
    extends RatesBoardService[F] {

  override def getRates: F[Map[Rate.Pair, Rate]] = cache.get

  private val allPairs: List[Rate.Pair] = Currency.allPairs.map(Rate.Pair.tupled)

  def backgroundStream(): Stream[F, Unit] = {
    def getAndSetRates(): F[Unit] = {
      val process: F[Unit] = oneFrame
        .getMany(allPairs)
        .flatMap[List[Rate]] {
          case Left(error: OneFrameUnknownError)      => Async[F].raiseError(new Exception(error.message))
          case Left(error: OneFrameUnreachableError)  => Async[F].raiseError(new Exception(error.message))
          case Left(error: OneFrameTimeoutError.type) => Async[F].raiseError(new Exception(error.message))
          case Right(value: List[Rate])               => Async[F].pure(value)
        }
        .map((rates: List[Rate]) => rates.map((rate: Rate) => rate.pair -> rate).toMap)
        .flatMap((newMap: Map[Rate.Pair, Rate]) => cache.set(newMap))

      process.handleErrorWith { t: Throwable =>
        Logger[F].error(t)("An exception occurred in RatesBoard!")
      }
    }

    Stream.eval(getAndSetRates()) >> Stream.awakeEvery[F](sleepDuration) >> Stream.eval(getAndSetRates())
  }
}

object LiveCachedRatesBoard {
  def apply[F[_]: Async: Timer: Logger](oneFrame: OneFrameService[F],
                                        cache: Ref[F, Map[Rate.Pair, Rate]],
                                        sleepDuration: FiniteDuration): LiveCachedRatesBoard[F] =
    new LiveCachedRatesBoard(oneFrame, cache, sleepDuration)
}
