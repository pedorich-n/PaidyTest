package forex.http.oneframe

import scala.util.Try

import java.time.OffsetDateTime

import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.oneframe.errors.OneFrameServiceErrorResponse
import io.circe.{ Decoder, HCursor }
import io.circe.generic.semiauto._

object Protocol {

  implicit val currencyDecoder: Decoder[Currency] = Decoder.decodeString.emapTry(Currency.withNameEither(_).toTry)
  implicit val timestampDecoder: Decoder[OffsetDateTime] =
    Decoder.decodeString.emapTry((value: String) => Try(OffsetDateTime.parse(value)))

  implicit val rateDecoder: Decoder[Rate] = (cursor: HCursor) =>
    for {
      from <- cursor.downField("from").as[Currency]
      to <- cursor.downField("to").as[Currency]
      price <- cursor.downField("price").as[BigDecimal]
      timestamp <- cursor.downField("time_stamp").as[OffsetDateTime]
    } yield {
      Rate(Rate.Pair(from, to), Price(price), Timestamp(timestamp))
  }

  implicit val errorResponseDecoder: Decoder[OneFrameServiceErrorResponse] = deriveDecoder[OneFrameServiceErrorResponse]

  implicit val rateOrErrorDecoder: Decoder[Either[OneFrameServiceErrorResponse, List[Rate]]] =
    errorResponseDecoder.either(Decoder.decodeList(rateDecoder))
}
