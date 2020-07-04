package forex.services.rates.interpreters

import java.time.{ ZoneOffset, ZonedDateTime }

import forex.services.rates.DateTimeProvider

class DefaultDateTimeProvider extends DateTimeProvider {
  override def getNowLocal: ZonedDateTime = ZonedDateTime.now()
  override def getNowUTC: ZonedDateTime   = getNowLocal.withZoneSameInstant(ZoneOffset.UTC)
}
