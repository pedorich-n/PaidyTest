package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpServerConfig,
    oneFrame: OneFrameConfig,
    threading: ThreadingConfig
)

case class HttpServerConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration,
    connectionPoolSize: Int
)

case class HttpOneFrameConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class OneFrameConfig(http: HttpOneFrameConfig, retryPolicy: RetryPolicy, token: String, rateExpiration: FiniteDuration)

case class RetryPolicy(maxRetries: Int, delay: FiniteDuration)

case class ThreadingConfig(main: ThreadingConfigEntry)

case class ThreadingConfigEntry(maxPoolSize: Int, corePoolSize: Int, keepAlive: FiniteDuration, queueSize: Int)
