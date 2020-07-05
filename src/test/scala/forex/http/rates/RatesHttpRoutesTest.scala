package forex.http.rates

import java.time.{OffsetDateTime, ZoneOffset}

import org.http4s.circe._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Request, Response}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

import cats.effect.{IO, Sync}
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.programs.RatesProgram
import forex.programs.rates.errors.Error.RateLookupFailed
import forex.programs.rates.{errors, Protocol => RProtocol}
import forex.tools.AsyncIOSpec
import io.circe.Json
import io.circe.literal._

class RatesHttpRoutesTest extends AsyncWordSpecLike with Matchers with AsyncIOSpec {

  class MockedRatesProgram[F[_]: Sync](responses: Map[RProtocol.GetRatesRequest, Either[errors.Error, Rate]])
      extends RatesProgram[F] {
    override def get(request: RProtocol.GetRatesRequest): F[Either[errors.Error, Rate]] =
      Sync[F].delay(responses(request))
  }

  val responses: Map[RProtocol.GetRatesRequest, Either[errors.Error, Rate]] = Map(
    RProtocol.GetRatesRequest(Currency.AUD, Currency.CAD) -> Right(
      Rate(
        Rate.Pair(Currency.AUD, Currency.CAD),
        Price(BigDecimal(0.5D)),
        Timestamp(OffsetDateTime.of(2020, 7, 4, 10, 0, 0, 0, ZoneOffset.UTC))
      )
    ),
    RProtocol.GetRatesRequest(Currency.SGD, Currency.AUD) -> Left(RateLookupFailed("Rate is missing!"))
  )

  val program: MockedRatesProgram[IO] = new MockedRatesProgram[IO](responses)

  "RatesHttpRoutes" should {
    "return valid JSON response" in {
      val routes: RatesHttpRoutes[IO] = new RatesHttpRoutes[IO](program)
      val request: Request[IO]        = Request[IO](uri = uri"/rates?from=AUD&to=CAD")

      routes.routes.apply(request).value.flatMap { maybeResponse =>
        maybeResponse
          .map { response: Response[IO] =>
            response.as[Json]
          }
          .getOrElse(Sync[IO].raiseError(new Exception("Empty!")))
          .asserting { response: Json =>
            val expected: Json = json"""
                  {
                    "from": "AUD",
                    "to": "CAD",
                    "price": 0.5,
                    "timestamp": "2020-07-04T10:00:00Z"
                  }
                  """

            expected.equals(response) shouldBe true
          }
      }
    }

    "return error JSON response" in {
      val routes: RatesHttpRoutes[IO] = new RatesHttpRoutes[IO](program)
      val request: Request[IO]        = Request[IO](uri = uri"/rates?from=SGD&to=AUD")

      routes.routes.apply(request).value.flatMap { maybeResponse =>
        maybeResponse
          .map { response: Response[IO] =>
            response.as[Json]
          }
          .getOrElse(Sync[IO].raiseError(new Exception("Empty!")))
          .asserting { response: Json =>
            val expected: Json = json"""
                  {
                    "message": "Rate is missing!"
                  }
                  """

            expected.equals(response) shouldBe true
          }
      }
    }

    "return error JSON response in case of invalid query params" in {
      val routes: RatesHttpRoutes[IO] = new RatesHttpRoutes[IO](program)
      val request: Request[IO]        = Request[IO](uri = uri"/rates?from=UAH&to=TRY")

      routes.routes.apply(request).value.flatMap { maybeResponse =>
        maybeResponse
          .map { response: Response[IO] =>
            response.as[Json]
          }
          .getOrElse(Sync[IO].raiseError(new Exception("Empty!")))
          .asserting { response: Json =>
            val expected: Json = json"""
                  {
                    "message": "Unknown Currency UAH; Unknown Currency TRY"
                  }
                  """

            expected.equals(response) shouldBe true
          }
      }
    }
  }
}
