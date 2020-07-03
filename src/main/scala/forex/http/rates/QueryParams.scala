package forex.http.rates

import org.http4s.dsl.impl.ValidatingQueryParamDecoderMatcher
import org.http4s.{ParseFailure, QueryParamDecoder}

import cats.data.ValidatedNel
import cats.instances.option._
import cats.syntax.apply._
import cats.syntax.either._
import enumeratum.NoSuchMember
import forex.domain.Currency
import forex.programs.rates.Protocol.GetRatesRequest

object QueryParams {

  private[http] implicit val currencyQueryParam: QueryParamDecoder[Currency] = QueryParamDecoder[String].emap {
    input: String =>
      Currency
        .withNameEither(input)
        .leftMap { error: NoSuchMember[Currency] =>
          ParseFailure(s"Unknown Currency ${error.notFoundName}", error.getMessage())
        }
  }

  object FromQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("to")

  object FromAndToQueryParams {
    def find(name: String): Either[ParseFailure, Currency] =
      Currency
        .withNameEither(name)
        .leftMap { error: NoSuchMember[Currency] =>
          ParseFailure(s"Unknown Currency ${error.notFoundName}", error.getMessage())
        }
    def unapply(params: Map[String, collection.Seq[String]]): Option[ValidatedNel[ParseFailure, GetRatesRequest]] = {
      val from: Option[ValidatedNel[ParseFailure, Currency]] = FromQueryParam.unapply(params)
      val to: Option[ValidatedNel[ParseFailure, Currency]]   = ToQueryParam.unapply(params)

      (to, from).mapN { case (t, f) => (t, f).mapN(GetRatesRequest) }

    }
  }

}
