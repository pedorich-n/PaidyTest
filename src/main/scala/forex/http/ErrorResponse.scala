package forex.http
import io.circe.Encoder
import io.circe.generic.semiauto._

/**
 * This model used to represent client-facing error, that will be returned  as JSON in case of error response
 * @param message message to return to client
 */
final case class ErrorResponse(message: String)

object ErrorResponse {
  implicit val errorResponseEncoder: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
}
