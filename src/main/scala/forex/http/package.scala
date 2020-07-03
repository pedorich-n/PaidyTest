package forex

import org.http4s.circe._
import org.http4s.{ EntityDecoder, EntityEncoder }

import cats.effect.Sync
import io.circe.generic.extras.decoding.UnwrappedDecoder
import io.circe.generic.extras.encoding.UnwrappedEncoder
import io.circe.{ Decoder, Encoder }

package object http {
  implicit def valueClassEncoder[A: UnwrappedEncoder]: Encoder[A] = implicitly
  implicit def valueClassDecoder[A: UnwrappedDecoder]: Decoder[A] = implicitly

  implicit def jsonDecoder[A <: Product: Decoder, F[_]: Sync]: EntityDecoder[F, A] = jsonOf[F, A]
  implicit def jsonEncoder[A <: Product: Encoder, F[_]: Sync]: EntityEncoder[F, A] = jsonEncoderOf[F, A]


}
