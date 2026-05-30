package parallel

import java.util.concurrent.*

opaque type Par[A] = ExecutorService => Future[A]

object Par:
  /** unit is represented as a function that returns a UnitFuture */
  def unit[A](a: A): Par[A] = es => UnitFuture(a)

  /** simple implementation of Future that just wraps a constant value. It
    * doesn’t use the ExecutorService at all; it’s always done and can’t be
    * cancelled. Its get method simply returns the value we gave it.
    */
  private case class UnitFuture[A](get: A) extends Future[A]:
    def isDone = true
    def isCancelled = false
    def get(timeout: Long, unit: TimeUnit): A = get
    def cancel(mayInterruptIfRunning: Boolean): Boolean = false

  private class Map2Future[A, B, C](
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

  extension [A](pa: Par[A])
    def run(s: ExecutorService): Future[A] = pa(s)

    /** map2 doesn’t evaluate the call to f in a separate logical thread, in
      * accord with our design choice of having fork be the sole function in the
      * API for controlling parallelism. We can do [[fork(pa.map2(pb)(f))]] if
      * we want the evaluation of f to occur in a separate thread.
      *
      * This implementation of map2 does not respect timeouts. To respect
      * timeouts, we’d need a new Future implementation that records the amount
      * of time spent evaluating af and then subtracts that time from the
      * available time allocated for evaluating bf.
      */
    def map2[B, C](pb: Par[B])(f: (A, B) => C): Par[C] =
      es => UnitFuture(f(pa(es).get, pb(es).get))

    def map2Timeouts[B, C](pb: Par[B])(f: (A, B) => C): Par[C] =
      es => Map2Future(pa(es), pb(es), f)

  /** Wraps a by-name value `a` into a `Par` that evaluates it in a separate
    * thread via `fork(unit(a))`. The computation is submitted to an
    * `ExecutorService` via `fork`, meaning it runs in a separate thread. Use
    * `lazyUnit` when you have an expression you want to evaluate in parallel
    * without blocking the caller. Ideal for async! look at [[asyncF]]
    */
  def lazyUnit[A](a: => A): Par[A] = fork(unit(a))

  /** This is the simplest and most natural implementation of fork, but there
    * are some problems with it; for one, the outer Callable will block waiting
    * for the inner task to complete. Since this blocking occupies a thread in
    * our thread pool, or whatever resource backs the ExecutorService, this
    * implies we’re losing out on some potential parallelism. Essentially, we’re
    * using two threads when one should suffice. This is a symptom of a more
    * serious problem with the implementation that we’ll discuss later in the
    * chapter.
    */
  def fork[A](a: => Par[A]): Par[A] =
    es =>
      es.submit(
        new Callable[A]:
          def call = a(es).get
      )

  /** convert any A => B to one that evaluates its result asynchronously */
  def asyncF[A, B](f: A => B): A => Par[B] =
    a => lazyUnit(f(a))
