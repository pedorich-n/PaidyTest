package forex

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }

import java.util.concurrent.Executors

import org.http4s.server.blaze.BlazeServerBuilder

import cats.effect._
import forex.config._
import fs2.Stream
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.{ Logger, SelfAwareStructuredLogger }

object Main extends IOApp {
  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val executionContext: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  override protected implicit def contextShift: ContextShift[IO] = IO.contextShift(executionContext)
  override protected implicit def timer: Timer[IO]               = IO.timer(executionContext)

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO](executionContext).stream.compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer: Logger](ec: ExecutionContext) {

  def stream: Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      module = new Module[F](config)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .withoutBanner
            .serve
            .concurrently(module.stream)
    } yield ()

}
