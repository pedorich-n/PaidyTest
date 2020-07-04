package forex.services.rates

import java.time.ZonedDateTime

trait DateTimeProvider {
  def getNowLocal: ZonedDateTime
  def getNowUTC: ZonedDateTime
}
