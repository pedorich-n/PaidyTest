package forex.http
import io.circe.Encoder
import io.circe.generic.semiauto._

final case class ErrorResponse(message: String)

object ErrorResponse {
  implicit val errorResponseEncoder: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
}
