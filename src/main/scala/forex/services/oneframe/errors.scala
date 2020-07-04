package forex.services.oneframe

object errors {

  sealed trait Error
  object Error {
    final case object OneFrameTimeoutError extends Error {
      val message: String = "OneFrame request timed out!"
    }
    final case class OneFrameUnreachableError(message: String) extends Error
    final case class OneFrameUnknownError(message: String) extends Error
  }

  final case class OneFrameServiceErrorResponse(error: String) extends Exception(error)

}
