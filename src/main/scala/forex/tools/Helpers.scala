package forex.tools

import scala.concurrent.ExecutionContext

import java.util.concurrent.{ ArrayBlockingQueue, ThreadFactory, ThreadPoolExecutor, TimeUnit }

import com.google.common.util.concurrent.ThreadFactoryBuilder
import forex.config.ThreadingConfigEntry

object Helpers {

  // $COVERAGE-OFF$
  /**
    * Creates ExecutionContext using given config
    * @param config ThreadingConfigEntry instance
    * @param namePattern naming patter, if has `%d` in it, it will be replaced with the number of thread in the pool
    * @param priority Thread priority in this pool
    */
  def getEcFromThreadingConfig(
      config: ThreadingConfigEntry,
      namePattern: String,
      priority: Int = Thread.NORM_PRIORITY
  ): ExecutionContext = {
    val threadFactory: ThreadFactory = new ThreadFactoryBuilder()
      .setNameFormat(namePattern)
      .setDaemon(false)
      .setPriority(priority)
      .build()

    ExecutionContext.fromExecutor {
      new ThreadPoolExecutor(
        config.corePoolSize,
        config.maxPoolSize,
        config.keepAlive.toSeconds,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue[Runnable](config.queueSize),
        threadFactory
      )
    }

  }
  // $COVERAGE-ON$

}
