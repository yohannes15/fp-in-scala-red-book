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
    def map[B](f: A => B): Par[B] = pa.map2(unit(()))((a, _) => f(a))

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

    def map3[B, C, D](pb: Par[B], pc: Par[C])(f: (A, B, C) => D): Par[D] =
      ???

    def map3Timouts[B, C, D](
        pb: Par[B],
        pc: Par[C]
    )(f: (A, B, C) => D): Par[D] =
      ???

    def map4[B, C, D, E](
        pb: Par[B],
        pc: Par[C],
        pd: Par[D]
    )(f: (A, B, C, D) => E): Par[E] =
      ???

    def map4Timeouts[B, C, D, E](
        pb: Par[B],
        pc: Par[C],
        pd: Par[D]
    )(f: (A, B, C, D) => E): Par[E] =
      ???

    def map5[B, C, D, E, F](
        pb: Par[B],
        pc: Par[C],
        pd: Par[D],
        pe: Par[E]
    )(f: (A, B, C, D, E) => F): Par[F] =
      ???

    def map5Timeouts[B, C, D, E, F](
        pb: Par[B],
        pc: Par[C],
        pd: Par[D],
        pe: Par[E]
    )(f: (A, B, C, D, E) => F): Par[F] =
      ???

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

  def sequence[A](ps: List[Par[A]]): Par[List[A]] =
    sequenceBalanced(ps.toIndexedSeq).map(_.toList)
    // ps.foldRight(unit(Nil)) { case (pa, acc) => pa.map2(acc)(_ :: _) }

  def sequenceBalanced[A](pas: IndexedSeq[Par[A]]): Par[IndexedSeq[A]] =
    if pas.isEmpty then
      unit(IndexedSeq.empty)
    else if pas.size == 1 then
      pas.head.map(a => IndexedSeq(a))
    else
      val (l, r) = pas.splitAt(pas.size / 2)
      sequenceBalanced(l).map2(sequenceBalanced(r))(_ ++ _)

  def parMap[A, B](as: List[A])(f: A => B): Par[List[B]] =
    fork {
      val fbs: List[Par[B]] = as.map(asyncF(f))
      sequence(fbs)
    }

  def parFilter[A](as: List[A])(f: A => Boolean): Par[List[A]] =
    fork {
      val pars = as.map(asyncF(a => if f(a) then List(a) else Nil))
      sequence(pars).map(_.flatten)
    }

  def parFold[A](ints: IndexedSeq[A])(z: A)(f: (A, A) => A): Par[A] =
    if ints.isEmpty then Par.unit(z)
    else if ints.size == 1 then Par.unit(f(z, ints.head))
    else
      val (l, r) = ints.splitAt(ints.size / 2)
      val parL = Par.fork(parFold(l)(z)(f))
      val parR = Par.fork(parFold(r)(z)(f))
      parL.map2(parR)(f)

  def max(ints: IndexedSeq[Int]): Par[Int] =
    parFold(ints)(Int.MinValue)(_ max _)

  def sum(ints: IndexedSeq[Int]): Par[Int] =
    Par.parFold(ints)(0)(_ + _)

object Examples:
  def sum(ints: IndexedSeq[Int]): Par[Int] =
    if ints.size <= 1 then
      Par.unit(ints.headOption.getOrElse(0))
    else
      val (l, r) = ints.splitAt(ints.size / 2)
      val parL = Par.fork(sum(l))
      val parR = Par.fork(sum(r))
      parL.map2(parR)(_ + _)

  def sortPar(parList: Par[List[Int]]) =
    parList.map(_.sorted)

  def totalNumberOfWords(paragraphs: List[String]): Par[Int] =
    ???
