package forex.domain

import cats.Show

sealed trait Currency extends Product with Serializable

object Currency {
  case object AUD extends Currency
  case object CAD extends Currency
  case object CHF extends Currency
  case object EUR extends Currency
  case object GBP extends Currency
  case object NZD extends Currency
  case object JPY extends Currency
  case object SGD extends Currency
  case object USD extends Currency

  implicit val show: Show[Currency] = Show.show {
    case AUD => "AUD"
    case CAD => "CAD"
    case CHF => "CHF"
    case EUR => "EUR"
    case GBP => "GBP"
    case NZD => "NZD"
    case JPY => "JPY"
    case SGD => "SGD"
    case USD => "USD"
  }

  def fromString(s: String): Currency = s.toUpperCase match {
    case "AUD" => AUD
    case "CAD" => CAD
    case "CHF" => CHF
    case "EUR" => EUR
    case "GBP" => GBP
    case "NZD" => NZD
    case "JPY" => JPY
    case "SGD" => SGD
    case "USD" => USD
  }

  val allPairs: List[(Currency, Currency)] = {
    List(AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD)
      .combinations(2)
      .flatMap(_.permutations)
      .collect { case List(from: Currency, to: Currency) => (from, to) }
      .toList
  }
}