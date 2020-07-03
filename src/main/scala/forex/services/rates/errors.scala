package forex.services.rates

object errors {

  sealed trait Error
  object Error {
    final case class LookupFailed(msg: String) extends Error
  }

}
