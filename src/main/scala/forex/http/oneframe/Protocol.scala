package forex.http.oneframe

import scala.util.Try

import java.time.OffsetDateTime

import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.oneframe.errors.OneFrameServiceError
import io.circe.{ Decoder, HCursor }

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

  implicit val errorDecoder: Decoder[OneFrameServiceError] = (c: HCursor) =>
    Decoder[String].at("message")(c).map(OneFrameServiceError) // TODO: Why deriveDecoder fails?

  implicit val rateOrErrorDecoder: Decoder[Either[OneFrameServiceError, List[Rate]]] =
    errorDecoder.either(Decoder.decodeList(rateDecoder))
}
