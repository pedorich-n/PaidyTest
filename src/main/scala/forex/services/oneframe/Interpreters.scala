package forex.services.oneframe

import cats.effect.{ Async, Timer }
import forex.config.OneFrameConfig
import forex.services.OneFrameService
import forex.services.oneframe.interpreters.OneFrameLive
import io.chrisdavenport.log4cats.Logger
import sttp.client.{ Identity, NothingT, SttpBackend }

object Interpreters {

  def live[F[_]: Async: Timer: Logger](config: OneFrameConfig,
                                       backend: SttpBackend[Identity, Nothing, NothingT],
                                       tokenProvider: OneFrameTokenProvider): OneFrameService[F] =
    OneFrameLive(config, backend, tokenProvider)
}
