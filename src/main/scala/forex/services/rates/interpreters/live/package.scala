package forex.services.rates.interpreters

import sttp.client.{NothingT, SttpBackend}

package object live {

  type EffectSttpBackend[F[_]] = SttpBackend[F, Nothing, NothingT]
}
