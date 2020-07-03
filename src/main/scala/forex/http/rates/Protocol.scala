package forex.http
package rates

import forex.domain.Rate.Pair
import forex.domain._
import io.circe._
import io.circe.generic.extras.semiauto.deriveUnwrappedEncoder
import io.circe.generic.semiauto._

object Protocol {

  final case class GetApiRequest(
      from: Currency,
      to: Currency
  )

  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timestamp: Timestamp
  )

  implicit val currencyEncoder: Encoder[Currency]       = Currency.circeEncoder
  implicit val priceEncoder: Encoder[Price]             = deriveUnwrappedEncoder[Price]
  implicit val timestampEncoder: Encoder[Timestamp]     = deriveUnwrappedEncoder[Timestamp]
  implicit val pairEncoder: Encoder[Pair]               = deriveEncoder[Pair]
  implicit val rateEncoder: Encoder[Rate]               = deriveEncoder[Rate]
  implicit val responseEncoder: Encoder[GetApiResponse] = deriveEncoder[GetApiResponse]

}
