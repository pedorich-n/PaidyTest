package forex.services.oneframe.interpreters

import forex.services.oneframe.OneFrameTokenProvider

/**
 * Default TokenProvider implementation, serves static token
 * @param token Token to return
 */
class StaticTokenProvider(token: String) extends OneFrameTokenProvider {
  override def getToken: String = token
}
