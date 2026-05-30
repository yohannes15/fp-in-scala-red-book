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

  extension [A](pa: Par[A])
    /** run is where the parallelism actually gets implemented, everything else
      * in [[Par]] is a description of parallel computation.
      */
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
      es =>
        val futureA = pa(es)
        val futureB = pb(es)
        UnitFuture(f(futureA.get, futureB.get))

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
