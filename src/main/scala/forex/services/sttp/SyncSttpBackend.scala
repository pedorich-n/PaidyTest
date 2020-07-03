package forex.services.sttp

import cats.effect.Sync
import sttp.client.{Identity, Request, Response, SttpBackend}
import sttp.client.monad.MonadError
import sttp.client.ws.WebSocketResponse

/**
 * Just a cats-effect [[Sync]] wrapper for an Identity [[SttpBackend]]
 * @param underlying Actual backend that will be used for requests
 */
class SyncSttpBackend[F[_]: Sync, S, WS[_]](underlying: SttpBackend[Identity, S, WS]) extends SttpBackend[F, S, WS] {
  override def send[T](request: Request[T, S]): F[Response[T]] = Sync[F].delay(underlying.send(request))

  override def openWebsocket[T, WS_RESULT](request: Request[T, S], handler: WS[WS_RESULT]): F[WebSocketResponse[WS_RESULT]] =
    Sync[F].delay(underlying.openWebsocket(request, handler))

  override def close(): F[Unit] = Sync[F].delay(underlying.close())

  private val monadError: MonadError[F]     = new SttpCatsMonadError[F]()
  override def responseMonad: MonadError[F] = monadError
}

object SyncSttpBackend {
  def apply[F[_]: Sync, S, WS[_]](underlying: SttpBackend[Identity, S, WS]): SyncSttpBackend[F, S, WS] = new SyncSttpBackend(underlying)
}
