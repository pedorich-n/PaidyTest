package forex.services.oneframe

object errors {

  sealed trait Error
  object Error {
    final case class OneFrameTimeout() extends Error
    final case class OneFrameCurrencyPair() extends Error
    final case class OneFrameUnknownError() extends Error
  }

  final case class OneFrameServiceError(message: String) extends Exception

}
