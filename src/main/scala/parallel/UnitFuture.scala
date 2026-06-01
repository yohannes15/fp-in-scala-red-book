package parallel

import java.util.concurrent.*

/** simple implementation of Future that just wraps a constant value. It
  * doesn't use the ExecutorService at all; it's always done and can't be
  * cancelled. Its get method simply returns the value we gave it.
  */
case class UnitFuture[A](get: A) extends Future[A]:
  def isDone = true
  def isCancelled = false
  def get(timeout: Long, unit: TimeUnit): A = get
  def cancel(mayInterruptIfRunning: Boolean): Boolean = false
