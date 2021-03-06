package forex

import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, ErrorAction, Timeout }

import _root_.sttp.client.{ HttpURLConnectionBackend, Identity, NothingT, SttpBackend }
import cats.effect.concurrent.Ref
import cats.effect.{ Concurrent, Sync, Timer }
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import forex.config.ApplicationConfig
import forex.domain.Rate
import forex.http.ErrorResponse
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import forex.services.oneframe.OneFrameTokenProvider
import forex.services.oneframe.interpreters.StaticTokenProvider
import forex.services.rates.interpreters.DefaultDateTimeProvider
import forex.services.ratesBoard.interpreters.LiveCachedRatesBoard
import forex.tools.WithLogger
import fs2.Stream

class Module[F[_]: Concurrent: Timer](config: ApplicationConfig) extends WithLogger[F] {

  override implicit protected def sync: Sync[F] = implicitly[Sync[F]]

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val oneFrame: OneFrameService[F] = {
    val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()
    val tokenProvider: OneFrameTokenProvider              = new StaticTokenProvider(config.oneFrame.staticToken)
    OneFrameService.live[F](config.oneFrame, backend, tokenProvider)
  }

  private val ref: Ref[F, Map[Rate.Pair, Rate]] = Ref.unsafe(Map.empty[Rate.Pair, Rate]) //TODO: safely create Ref
  private val board: LiveCachedRatesBoard[F]    = RatesBoardService.live[F](oneFrame, ref, config.oneFrame.ratesRefresh)

  private val ratesService: RatesService[F] =
    RatesServices.live[F](board, config.ratesExpiration, new DefaultDateTimeProvider())

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  private val routesMiddleware: PartialMiddleware = { http: HttpRoutes[F] =>
    AutoSlash(http)
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    val logError: (Throwable, => String) => F[Unit] = (t, m) => getLogger.flatMap(_.error(t)(m))
    ErrorAction
      .log(Timeout(config.http.timeout)(http), logError, logError)
      .handleError { t: Throwable =>
        Response(Status.InternalServerError).withEntity(ErrorResponse(t.getMessage))
      }
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

  val concurrentStream: Stream[F, Unit] = board.updaterStream()

}
