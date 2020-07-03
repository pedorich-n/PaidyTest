package forex.config

import cats.effect.Sync
import fs2.Stream
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

object Config {

  /**
   * Loads config at the default Source or throws an error
   * @param path the property path inside the default configuration
   */
  @throws[ConfigReaderException[ApplicationConfig]]
  def loadUnsafe(path: String): ApplicationConfig = ConfigSource.default.at(path).loadOrThrow[ApplicationConfig]

  /**
   * @param path the property path inside the default configuration
   */
  def stream[F[_]: Sync](path: String): Stream[F, ApplicationConfig] = {
    Stream.eval(Sync[F].delay(
      ConfigSource.default.at(path).loadOrThrow[ApplicationConfig]))
  }

}
