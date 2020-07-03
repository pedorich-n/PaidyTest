package forex.services.sttp

import cats.MonadError
import sttp.client.monad.{MonadError => SttpMonadError}

/**
 * Interop between [[sttp.client.monad.MonadError]] and [[cats.MonadError]]
 * @param monadError implicit instance of cats' MonadError[F, Throwable]
 * @tparam F Higher-kinded type with `MonadError[F, Throwable]` defined
 */
class SttpCatsMonadError[F[_]](implicit monadError: MonadError[F, Throwable]) extends SttpMonadError[F] {
  override def unit[T](t: T): F[T] = monadError.pure(t)

  override def map[T, T2](fa: F[T])(f: T => T2): F[T2] = monadError.map(fa)(f)

  override def flatMap[T, T2](fa: F[T])(f: T => F[T2]): F[T2] = monadError.flatMap(fa)(f)

  override def error[T](t: Throwable): F[T] = monadError.raiseError(t)

  override protected def handleWrappedError[T](rt: F[T])(h: PartialFunction[Throwable, F[T]]): F[T] = monadError.handleErrorWith(rt)(h)
}
