package forex.services.oneframe.interpreters

import cats.effect.{ Async, Timer }
import forex.config.OneFrameConfig
import forex.domain.Rate
import forex.services.oneframe.Algebra
import forex.services.rates.interpreters.live.OneFrameTokenProvider
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.{ Logger, SelfAwareStructuredLogger }
import retry._
import sttp.client.SttpBackend
import sttp.client._
import io.circe.generic.auto._
import com.softwaremill.sttp.circe._
import io.circe.parser._
import sttp.model.Uri
import cats.syntax.flatMap._
import cats.syntax.functor._

class OneFrameLive[F[_]: Async: Timer: Logger](config: OneFrameConfig,
                                               backend: SttpBackend[F, _, _],
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
    getLogger.flatMap { logger =>
      details match {
        case RetryDetails.GivingUp(totalRetries, _) =>
          logger.error(throwable)(s"Giving up retrying to get data from OneFrame, after $totalRetries retries")
        case RetryDetails.WillDelayAndRetry(nextDelay, _, _) =>
          logger.warn(s"Error getting data from OneFrame, will retry in ${nextDelay.toSeconds} seconds")
      }
    }

  override def getMany(pairs: Seq[Rate.Pair]): F[List[Rate]] = {
    val params: Seq[(String, String)] = pairs.map((pair: Rate.Pair) => "pair" -> s"${pair.from}${pair.to}")
    val url: Uri                      = uri"http://${config.http.host}:${config.http.port}?$params"
    val request = backend.send(baseRequest.readTimeout(config.http.timeout).get(url)).flatMap {
      response: Response[Either[String, String]] =>
        //Using explicit conversion to JSON because OneFrame might return error with status code 200 OK
        response.body match {
          case Left(value)  =>
          case Right(value) => decode[Seq[Rate]]
        }

    }
  }

  override def get(pair: Rate.Pair): F[Rate] = ???
}
