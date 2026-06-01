package parallel

import java.util.concurrent.*

/** A Future implementation for map2 that properly respects timeouts by tracking
  * elapsed time when awaiting each side.
  */
class Map2Future[A, B, C](
    aFuture: Future[A],
    bFuture: Future[B],
    f: (A, B) => C
) extends Future[C]:
  // Cache needed for isDone. Any future needs to answer "are you done?" honestly
  @volatile private var cache: Option[C] = None

  def isDone: Boolean = cache.isDefined
  def get(): C = get(Long.MaxValue, TimeUnit.NANOSECONDS)
  def get(timeout: Long, unit: TimeUnit): C =
    val timeoutNs = TimeUnit.NANOSECONDS.convert(timeout, unit)
    val started = System.nanoTime
    val a = aFuture.get(timeoutNs, TimeUnit.NANOSECONDS)
    val elapsed = System.nanoTime - started
    val b = bFuture.get(timeoutNs - elapsed, TimeUnit.NANOSECONDS)
    val c = f(a, b)
    cache = Some(c)
    c

  def isCancelled: Boolean = aFuture.isCancelled || bFuture.isCancelled
  def cancel(evenIfRunning: Boolean): Boolean =
    aFuture.cancel(evenIfRunning) || bFuture.cancel(evenIfRunning)
