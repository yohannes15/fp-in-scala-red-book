package parallel

import java.util.concurrent.*

opaque type Par[A] = ExecutorService => Future[A]

object Par:
  /** unit is represented as a function that returns a UnitFuture */
  def unit[A](a: A): Par[A] = es => UnitFuture(a)

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
      val combined: Par[C => D] = pa.map2(pb) {
        (a, b) => (c: C) => f(a, b, c)
      }
      combined.map2(pc)((g, c) => g(c))

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

  def delay[A](fa: => Par[A]): Par[A] =
    es => fa(es)

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
    parFoldMap(ints)(z)(identity[A])(f)

  /** generalizing the map-transform-combine pattern */
  def parFoldMap[A, B](as: IndexedSeq[A])(z: B)(
      f: A => B
  )(g: (B, B) => B): Par[B] =
    if as.isEmpty then Par.unit(z)
    else if as.size == 1 then Par.unit(f(as.head))
    else
      val (l, r) = as.splitAt(as.size / 2)
      val parL = Par.fork(parFoldMap(l)(z)(f)(g))
      val parR = Par.fork(parFoldMap(r)(z)(f)(g))
      parL.map2(parR)(g)

  def choice[A](cond: Par[Boolean])(t: Par[A], f: Par[A]): Par[A] =
    es =>
      // Notice we are blocking on the result of `cond`.
      if cond.run(es).get then t(es)
      else f(es)

  def choiceN[A](n: Par[Int])(choices: List[Par[A]]): Par[A] =
    es =>
      val ind = n.run(es).get % choices.size 
      choices(ind).run(es)

  def choiceViaChoiceN[A](cond: Par[Boolean])(t: Par[A], f: Par[A]): Par[A] =
    choiceN(cond.map(b => if b then 0 else 1))(List(t, f))

  def max(ints: IndexedSeq[Int]): Par[Int] =
    parFold(ints)(Int.MinValue)(_ max _)

  def sum(ints: IndexedSeq[Int]): Par[Int] =
    parFold(ints)(0)(_ + _)

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
    // Par.parMap(paragraphs)(p => p.split("\\s+").size).map(_.sum)
    Par.parFoldMap(paragraphs.toIndexedSeq)(0)(_.split("\\s+").size)(_ + _)
