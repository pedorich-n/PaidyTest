package forex.http
package rates

import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{HttpRoutes, ParseFailure}

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.flatMap._
import forex.domain.Rate
import forex.programs.RatesProgram
import forex.programs.rates.errors.Error.RateLookupFailed
import forex.programs.rates.{Protocol => RatesProgramProtocol}

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._
  import Protocol._
  import QueryParams._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromAndToQueryParams(validatedParams) =>
      validatedParams.fold(
        (nel: NonEmptyList[ParseFailure]) => BadRequest(ErrorResponse(nel.map(_.sanitized).toList.mkString("; "))),
        (request: RatesProgramProtocol.GetRatesRequest) =>
          rates.get(request).flatMap {
            case Left(error: RateLookupFailed) => InternalServerError(ErrorResponse(error.msg))
            case Right(rate: Rate)         => Ok(rate.asGetApiResponse)
        }
      )
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
