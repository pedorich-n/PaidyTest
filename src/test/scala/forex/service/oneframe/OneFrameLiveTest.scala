package forex.service.oneframe

import scala.concurrent.duration.FiniteDuration

import java.time.OffsetDateTime

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

import cats.effect.IO
import forex.config.{ HttpOneFrameConfig, OneFrameConfig, RetryPolicy }
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.oneframe.errors.Error.OneFrameUnknownError
import forex.services.oneframe.interpreters.{ OneFrameLive, StaticTokenProvider }
import forex.tools.AsyncIOSpec
import sttp.client.testing.SttpBackendStub
import sttp.client.{ HttpURLConnectionBackend, Identity, NothingT, Request, SttpBackend }
import sttp.model.Header

class OneFrameLiveTest extends AsyncWordSpecLike with Matchers with AsyncIOSpec {

  val stubBackend: SttpBackendStub[Identity, Nothing, NothingT] = HttpURLConnectionBackend.stub

  val config: OneFrameConfig = OneFrameConfig(
    HttpOneFrameConfig("localhost", 8080, FiniteDuration(2, "minutes")),
    RetryPolicy(3, FiniteDuration(2, "seconds")),
    "token",
    FiniteDuration(5, "minutes")
  )
  val tokenProvider: StaticTokenProvider = new StaticTokenProvider(config.staticToken)

  def matcher(pairs: Seq[Rate.Pair]): Request[_, _] => Boolean = { request =>
    val queryParams: String = {
      val builder: StringBuilder = StringBuilder.newBuilder
      builder.append(s"?pair=${pairs.head.from}${pairs.head.to}")
      pairs.drop(1).foreach(pair => builder.append(s"&pair=${pair.from}${pair.to}"))
      builder.result()
    }
    request.uri.toString() == s"http://localhost:8080/rates$queryParams" &&
    request.headers.contains(Header("token", config.staticToken))
  }

  "OneFrameLive" should {

    "return rates" in {
      val pairs: Seq[Rate.Pair] = Seq(Rate.Pair(Currency.USD, Currency.EUR), Rate.Pair(Currency.AUD, Currency.SGD))
      val response: String =
        """
          |[
          |  {
          |    "from": "USD",
          |    "to": "EUR",
          |    "bid": 0.8702743979669029,
          |    "ask": 0.8129834411047454,
          |    "price": 0.84162891953582415,
          |    "time_stamp": "2020-07-04T17:56:12.907Z"
          |  },
          |  {
          |    "from": "AUD",
          |    "to": "SGD",
          |    "bid": 0.5891192858066693,
          |    "ask": 0.8346334453420459,
          |    "price": 0.7118763655743576,
          |    "time_stamp": "2020-07-04T17:56:12.907Z"
          |  }
          |]
          |""".stripMargin

      val expected = Seq(
        Rate(
          Rate.Pair(Currency.USD, Currency.EUR),
          Price(BigDecimal("0.84162891953582415")),
          Timestamp(OffsetDateTime.parse("2020-07-04T17:56:12.907Z"))
        ),
        Rate(
          Rate.Pair(Currency.AUD, Currency.SGD),
          Price(BigDecimal("0.7118763655743576")),
          Timestamp(OffsetDateTime.parse("2020-07-04T17:56:12.907Z"))
        )
      )

      val stubbedQuery: SttpBackend[Identity, Nothing, NothingT] =
        stubBackend.whenRequestMatches(matcher(pairs)).thenRespond(response)

      new OneFrameLive[IO](config, stubbedQuery, tokenProvider).getMany(pairs).asserting { result =>
        result.map(_ should contain theSameElementsAs expected).right.get
      }
    }

    "retry multiple times if service fails" in {
      val pairs: Seq[Rate.Pair] = Seq(Rate.Pair(Currency.USD, Currency.EUR), Rate.Pair(Currency.AUD, Currency.SGD))

      val errorResponse: String =
        """
          |{
          |  "error": "Something bad happened"
          |}
          |""".stripMargin

      val response: String =
        """
          |[
          |  {
          |    "from": "USD",
          |    "to": "EUR",
          |    "bid": 0.8702743979669029,
          |    "ask": 0.8129834411047454,
          |    "price": 0.84162891953582415,
          |    "time_stamp": "2020-07-04T17:56:12.907Z"
          |  },
          |  {
          |    "from": "AUD",
          |    "to": "SGD",
          |    "bid": 0.5891192858066693,
          |    "ask": 0.8346334453420459,
          |    "price": 0.7118763655743576,
          |    "time_stamp": "2020-07-04T17:56:12.907Z"
          |  }
          |]
          |""".stripMargin

      val stubbedQuery: SttpBackend[Identity, Nothing, NothingT] =
        stubBackend.whenRequestMatches(matcher(pairs)).thenRespondCyclic(errorResponse, errorResponse, response)

      val expected = Seq(
        Rate(
          Rate.Pair(Currency.USD, Currency.EUR),
          Price(BigDecimal("0.84162891953582415")),
          Timestamp(OffsetDateTime.parse("2020-07-04T17:56:12.907Z"))
        ),
        Rate(
          Rate.Pair(Currency.AUD, Currency.SGD),
          Price(BigDecimal("0.7118763655743576")),
          Timestamp(OffsetDateTime.parse("2020-07-04T17:56:12.907Z"))
        )
      )

      new OneFrameLive[IO](config, stubbedQuery, tokenProvider).getMany(pairs).asserting { result =>
        result.map(_ should contain theSameElementsAs expected).right.get
      }
    }

    "return error in case service returned error" in {
      val response: String =
        """
          |{
          |  "error": "Something bad happened"
          |}
          |""".stripMargin

      val pairs: Seq[Rate.Pair] = Seq(Rate.Pair(Currency.USD, Currency.EUR), Rate.Pair(Currency.AUD, Currency.SGD))

      val stubbedQuery: SttpBackend[Identity, Nothing, NothingT] =
        stubBackend.whenRequestMatches(matcher(pairs)).thenRespond(response)

      new OneFrameLive[IO](config, stubbedQuery, tokenProvider).getMany(pairs).asserting { result =>
        result shouldBe Left(OneFrameUnknownError("Something bad happened"))
      }
    }

    "return error in case service returns unexpected model" in {
      val response: String      = "body"
      val pairs: Seq[Rate.Pair] = Seq(Rate.Pair(Currency.USD, Currency.EUR), Rate.Pair(Currency.AUD, Currency.SGD))

      val stubbedQuery: SttpBackend[Identity, Nothing, NothingT] =
        stubBackend.whenRequestMatches(matcher(pairs)).thenRespond(response)

      new OneFrameLive[IO](config, stubbedQuery, tokenProvider).getMany(pairs).asserting { result =>
        result shouldBe Left(OneFrameUnknownError("Circe failure! body"))
      }
    }

  }
}
