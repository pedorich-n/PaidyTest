package forex.domain

import scala.collection.immutable

import cats.Show
import enumeratum.EnumEntry.Uppercase
import enumeratum.{ CirceEnum, Enum, EnumEntry }

sealed trait Currency extends EnumEntry with Uppercase

object Currency extends Enum[Currency] with CirceEnum[Currency] {
  case object AUD extends Currency
  case object CAD extends Currency
  case object CHF extends Currency
  case object EUR extends Currency
  case object GBP extends Currency
  case object NZD extends Currency
  case object JPY extends Currency
  case object SGD extends Currency
  case object USD extends Currency

  override val values: immutable.IndexedSeq[Currency] = findValues

  implicit val show: Show[Currency] = Show.show(_.entryName)

  val allPairs: List[(Currency, Currency)] = {
    values
      .combinations(2)
      .flatMap(_.permutations)
      .collect { case Seq(from: Currency, to: Currency) => (from, to) }
      .toList
  }

}
