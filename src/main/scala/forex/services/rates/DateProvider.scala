package forex.services.rates

import java.time.OffsetDateTime

trait DateProvider {
  def getNow: OffsetDateTime
}
