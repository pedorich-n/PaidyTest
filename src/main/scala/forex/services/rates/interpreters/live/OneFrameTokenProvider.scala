package forex.services.rates.interpreters.live

trait OneFrameTokenProvider {
  def getToken: String
}

object OneFrameTokenProvider {
  class StaticTokenProvider(token: String) extends OneFrameTokenProvider {
    override def getToken: String = token
  }
}

