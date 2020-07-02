package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class OneFrameConfig(http: HttpConfig, retryPolicy: RetryPolicy)

case class RetryPolicy(maxRetries: Int, delay: FiniteDuration)