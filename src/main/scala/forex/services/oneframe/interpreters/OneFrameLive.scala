package forex.services.oneframe.interpreters

import cats.effect.{ Async, Timer }
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import forex.config.OneFrameConfig
import forex.domain.Rate
import forex.http.oneframe.Protocol._
import forex.services.oneframe.{ Algebra, OneFrameTokenProvider }
import forex.services.oneframe.errors.OneFrameServiceError
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.{ Logger, SelfAwareStructuredLogger }
import io.circe
import retry._
import sttp.client.circe._
import sttp.client.{ SttpBackend, _ }
import sttp.model.Uri

class OneFrameLive[F[_]: Async: Timer: Logger](config: OneFrameConfig,
                                               backend: SttpBackend[F, Nothing, NothingT],
                                               tokenProvider: OneFrameTokenProvider)
    extends Algebra[F] {

  private val getLogger: F[SelfAwareStructuredLogger[F]] = Slf4jLogger.create[F]

  private val retryPolicy: RetryPolicy[F] =
    RetryPolicies.limitRetries(config.retryPolicy.maxRetries) join RetryPolicies.constantDelay(config.retryPolicy.delay)

  private val headers = Map(
    "user-agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116",
    "accept" -> "application/json"
  )

  private def baseRequest: RequestT[Empty, Either[String, String], Nothing] =
    basicRequest.header("token", tokenProvider.getToken).headers(headers)

  private def logRetryError(throwable: Throwable, details: RetryDetails): F[Unit] =
    getLogger.flatMap { logger: SelfAwareStructuredLogger[F] =>
      details match {
        case RetryDetails.GivingUp(totalRetries, _) =>
          logger.error(throwable)(s"Giving up retrying to get data from OneFrame, after $totalRetries retries")
        case RetryDetails.WillDelayAndRetry(nextDelay, _, _) =>
          logger.warn(s"Error getting data from OneFrame, will retry in ${nextDelay.toSeconds} seconds")
      }
    }

  override def getMany(pairs: Seq[Rate.Pair]): F[Either[OneFrameServiceError, List[Rate]]] = {
    val params: Seq[(String, String)] = pairs.map((pair: Rate.Pair) => "pair" -> s"${pair.from}${pair.to}")
    val url: Uri                      = uri"${config.http.host}:${config.http.port}/rates?$params"

    val request: F[Either[OneFrameServiceError, List[Rate]]] = backend
      .send {
        baseRequest
          .readTimeout(config.http.timeout)
          .get(url)
          .response(asJson[Either[OneFrameServiceError, List[Rate]]])
      }
      .flatMap { response: Response[Either[ResponseError[circe.Error], Either[OneFrameServiceError, List[Rate]]]] =>
        response.body match {
          case Left(error: ResponseError[circe.Error])  => Left(OneFrameServiceError(error.body)).pure[F].widen
          case Right(Left(error: OneFrameServiceError)) => Left(error).pure[F].widen
          case Right(Right(rates: List[Rate])) =>
            getLogger.map(_.debug(s"Got response for ${rates.size} rates from OneFrame")) *> Right(rates).pure[F].widen
        }
      }

    retryingOnAllErrors(retryPolicy, logRetryError)(request)
  }
}

object OneFrameLive {
  def apply[F[_]: Async: Timer: Logger](config: OneFrameConfig,
                                        backend: SttpBackend[F, Nothing, NothingT],
                                        tokenProvider: OneFrameTokenProvider): OneFrameLive[F] =
    new OneFrameLive(config, backend, tokenProvider)
}
