package forex.services.oneframe

import forex.domain.Rate

trait Algebra[F[_]] {
  def getMany(pairs: Seq[Rate.Pair]): F[List[Rate]] //TODO: Error
  def get(pair: Rate.Pair): F[Rate] //TODO: Error
}
