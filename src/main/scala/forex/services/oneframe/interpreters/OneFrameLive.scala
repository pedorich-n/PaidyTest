package forex.services.oneframe.interpreters

import java.net.{ConnectException, SocketTimeoutException}

import cats.effect.{Async, Sync, Timer}
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import forex.config.OneFrameConfig
import forex.domain.Rate
import forex.http.oneframe.Protocol._
import forex.services.oneframe.errors.Error.{OneFrameTimeoutError, OneFrameUnknownError, OneFrameUnreachableError}
import forex.services.oneframe.errors.{Error, OneFrameServiceErrorResponse}
import forex.services.oneframe.{Algebra, OneFrameTokenProvider}
import forex.tools.WithLogger
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.circe.{Error => CError}
import retry._
import sttp.client._
import sttp.client.circe._
import sttp.model.Uri

/**
 * Live implementation of the OneFrame service that queries external service for getting the rates and supports
 * retries and error response handling
 * @param config OneFrame config
 * @param backend Sttp backend instance, internally wrapped into the `F`
 * @param tokenProvider Token provider for the OneFrame
 * @tparam F Effect type
 */
class OneFrameLive[F[_]: Async: Timer](config: OneFrameConfig,
                                       backend: SttpBackend[Identity, Nothing, NothingT],
                                       tokenProvider: OneFrameTokenProvider)
    extends Algebra[F]
    with WithLogger[F] {

  override implicit protected def sync: Sync[F] = implicitly[Sync[F]]

  private val retryPolicy: RetryPolicy[F] =
    RetryPolicies.limitRetries(config.retryPolicy.maxRetries) join RetryPolicies.constantDelay(config.retryPolicy.delay)

  private val headers = Map(
    "user-agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116",
    "accept" -> "application/json"
  )

  private def baseRequest: RequestT[Empty, Either[String, String], Nothing] =
    basicRequest.header("token", tokenProvider.getToken).headers(headers)

  private def logRetryError(details: RetryDetails): F[Unit] =
    getLogger.flatMap { logger: SelfAwareStructuredLogger[F] =>
      details match {
        case RetryDetails.GivingUp(totalRetries, _) =>
          logger.error(s"Giving up retrying to get data from OneFrame, after $totalRetries retries")
        case RetryDetails.WillDelayAndRetry(nextDelay, _, _) =>
          logger.warn(s"Error getting data from OneFrame, will retry in ${nextDelay.toSeconds} seconds")
      }
    }

  override def getMany(pairs: Seq[Rate.Pair]): F[Either[Error, List[Rate]]] = {
    val params: Seq[(String, String)] = pairs.map((pair: Rate.Pair) => "pair" -> s"${pair.from}${pair.to}")
    val url: Uri                      = uri"http://${config.http.host}:${config.http.port}/rates?$params"

    val request: F[Either[Error, List[Rate]]] = Async[F]
      .delay {
        backend
          .send {
            baseRequest
              .readTimeout(config.http.timeout)
              .get(url)
              .response(asJson[Either[OneFrameServiceErrorResponse, List[Rate]]])
          }
      }
      .map[Either[Error, List[Rate]]] {
        response: Response[Either[ResponseError[CError], Either[OneFrameServiceErrorResponse, List[Rate]]]] =>
          response.body match {
            case Left(error: ResponseError[CError])               => Left(OneFrameUnknownError(s"Circe failure! ${error.body}"))
            case Right(Left(error: OneFrameServiceErrorResponse)) => Left(OneFrameUnknownError(error.error))
            case Right(Right(rates: List[Rate]))                  => Right(rates)
          }
      }
      .handleError {
        case c: ConnectException       => Left(OneFrameUnreachableError(c.getMessage))
        case _: SocketTimeoutException => Left(OneFrameTimeoutError)
        case t: Throwable              => Left(OneFrameUnknownError(t.getMessage))
      }

    retryingM[Either[Error, List[Rate]]](retryPolicy, _.isRight, (_, details) => logRetryError(details))(request)
  }
}

object OneFrameLive {
  def apply[F[_]: Async: Timer](config: OneFrameConfig,
                                backend: SttpBackend[Identity, Nothing, NothingT],
                                tokenProvider: OneFrameTokenProvider): OneFrameLive[F] =
    new OneFrameLive(config, backend, tokenProvider)
}
