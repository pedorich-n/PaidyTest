package forex

import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, ErrorAction, Timeout }

import _root_.sttp.client.{ HttpURLConnectionBackend, NothingT }
import cats.effect.concurrent.Ref
import cats.effect.{ Concurrent, Timer }
import forex.config.ApplicationConfig
import forex.domain.Rate
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import forex.services.oneframe.interpreters.{ OneFrameLive, StaticTokenProvider }
import forex.services.oneframe.{ OneFrameTokenProvider, Algebra => OneFrameAlgebra }
import forex.services.rates.interpreters.DefaultDateProvider
import forex.services.ratesBoard.interpreters.LiveCachedRatesBoard
import forex.services.sttp.SyncSttpBackend
import fs2.Stream
import io.chrisdavenport.log4cats.Logger

class Module[F[_]: Concurrent: Timer: Logger](config: ApplicationConfig) {

  private val oneFrame: OneFrameAlgebra[F] = {
    val backend: SyncSttpBackend[F, Nothing, NothingT] =
      SyncSttpBackend[F, Nothing, NothingT](HttpURLConnectionBackend())
    val tokenProvider: OneFrameTokenProvider = new StaticTokenProvider(config.oneFrame.token)
    OneFrameLive[F](config.oneFrame, backend, tokenProvider)
  }

  val ref: Ref[F, Map[Rate.Pair, Rate]]      = Ref.unsafe(Map.empty[Rate.Pair, Rate])
  private val board: LiveCachedRatesBoard[F] = new LiveCachedRatesBoard[F](oneFrame, ref, config.oneFrame.cacheTtl)

  private val ratesService: RatesService[F] =
    RatesServices.live(board, config.oneFrame.cacheTtl, new DefaultDateProvider())

  private def ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = { http: HttpRoutes[F] =>
    AutoSlash(http)
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    val logError: (Throwable, => String) => F[Unit] = (t, m) => Logger[F].error(t)(m)
    ErrorAction.log(Timeout(config.http.timeout)(http), logError, logError)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

  val stream: Stream[F, Unit] = board.backgroundStream()

}
