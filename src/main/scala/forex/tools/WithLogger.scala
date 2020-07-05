package forex.tools

import cats.effect.Sync
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

/**
  * Adds a Log4Cats logger instance
  * @tparam F Effect type, must have a `Sync[F]` implementation available
  */
trait WithLogger[F[_]] {
  implicit protected def sync: Sync[F]
  protected val getLogger: F[SelfAwareStructuredLogger[F]] = Slf4jLogger.create[F]
}
