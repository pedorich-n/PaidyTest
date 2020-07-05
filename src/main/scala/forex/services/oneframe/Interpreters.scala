package forex.services.oneframe

import cats.effect.{ Async, Timer }
import forex.config.OneFrameConfig
import forex.services.OneFrameService
import forex.services.oneframe.interpreters.OneFrameLive
import sttp.client.{ Identity, NothingT, SttpBackend }

object Interpreters {

  def live[F[_]: Async: Timer](config: OneFrameConfig,
                               backend: SttpBackend[Identity, Nothing, NothingT],
                               tokenProvider: OneFrameTokenProvider): OneFrameService[F] =
    OneFrameLive(config, backend, tokenProvider)
}
