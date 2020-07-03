package forex.services.oneframe.interpreters

import forex.services.oneframe.OneFrameTokenProvider

class StaticTokenProvider(token: String) extends OneFrameTokenProvider {
  override def getToken: String = token
}
