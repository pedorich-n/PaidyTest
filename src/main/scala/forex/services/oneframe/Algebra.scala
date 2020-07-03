package forex.services.oneframe

import forex.domain.Rate
import forex.services.oneframe.errors.OneFrameServiceError

trait Algebra[F[_]] {
  def getMany(pairs: Seq[Rate.Pair]): F[Either[OneFrameServiceError, List[Rate]]]
}
