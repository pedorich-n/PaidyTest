package forex

package object services {
  type RatesService[F[_]] = rates.Algebra[F]
  final val RatesServices = rates.Interpreters

  type OneFrameService[F[_]] = oneframe.Algebra[F]
  final val OneFrameService = oneframe.Interpreters

  type RatesBoardService[F[_]] = ratesBoard.Algebra[F]
  final val RatesBoardService = ratesBoard.Interpreters
}
