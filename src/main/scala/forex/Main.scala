package forex

import scala.concurrent.ExecutionContext

import org.http4s.server.blaze.BlazeServerBuilder

import cats.effect._
import forex.config._
import forex.tools.Helpers
import fs2.Stream

object Main extends IOApp {

  /**
    * Need to use unsafe load in order to override ContextShift provided by IOApp.
    */
  val config: ApplicationConfig = Config.loadUnsafe("app")

  val executionContext: ExecutionContext = Helpers.getEcFromThreadingConfig(config.threading.main, "main-io-%d")

  override protected implicit def contextShift: ContextShift[IO] = IO.contextShift(executionContext)
  override protected implicit def timer: Timer[IO]               = IO.timer(executionContext)

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO](executionContext, config).stream.compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer](ec: ExecutionContext, config: ApplicationConfig) {

  def stream: Stream[F, ExitCode] = {
    val module = new Module[F](config)
    BlazeServerBuilder[F](ec)
      .bindHttp(config.http.port, config.http.host)
      .withConnectorPoolSize(config.http.connectionPoolSize)
      .withHttpApp(module.httpApp)
      .withoutBanner
      .serve
      .concurrently(module.concurrentStream)
  }

}
