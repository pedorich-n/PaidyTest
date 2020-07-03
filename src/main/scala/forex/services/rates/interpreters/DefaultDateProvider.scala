package forex.services.rates.interpreters

import java.time.OffsetDateTime

import forex.services.rates.DateProvider

class DefaultDateProvider  extends DateProvider  {
  override def getNow: OffsetDateTime = OffsetDateTime.now()
}
