package parallel.nonblocking

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference

object Par:

  opaque type Future[+A] = (A => Unit) => Unit
  opaque type Par[+A] = ExecutorService => Future[A]

  extension [A](pa: Par[A])
    def run(es: ExecutorService): A =
      // A mutable, threadsafe reference, to use for storing the result
      val ref = new AtomicReference[A]
      // A latch which, when decremented, implies that `ref` has the result
      val latch = new CountDownLatch(1)
      // Asynchronously set the result, and decrement the latch
      pa(es) { a => ref.set(a); latch.countDown }
      // Block until the `latch.countDown` is invoked asynchronously
      latch.await
      // Once we've passed the latch, we know `ref` has been set, and return its value
      ref.get

    def map[B](f: A => B): Par[B] =
      es => cb => pa(es)(a => eval(es)(cb(f(a))))

    def map2[B, C](pb: Par[B])(f: (A, B) => C): Par[C] =
      es =>
        cb =>
          var ar: Option[A] = None
          var br: Option[B] = None
          // this implementation is a little too liberal in forking of threads -
          // it forks a new logical thread for the actor and for stack-safety,
          // forks evaluation of the callback `cb`
          val combiner = Actor[Either[A, B]](es):
            case Left(a) =>
              if br.isDefined then Par.eval(es)(cb(f(a, br.get)))
              else ar = Some(a)
            case Right(b) =>
              if ar.isDefined then Par.eval(es)(cb(f(ar.get, b)))
              else br = Some(b)
          pa(es)(a => combiner ! Left(a))
          pb(es)(b => combiner ! Right(b))

    def flatMap[B](f: A => Par[B]): Par[B] =
      // Note: fork isn't strictly necessary but lets us avoid stack overflows
      // when chaining lots of flatMap calls - we use this stack safety in part 4
      fork(es => cb => pa(es)(a => f(a)(es)(cb)))

    def zip[B](pb: Par[B]): Par[(A, B)] =
      pa.map2(pb)((_, _))

  def unit[A](a: A): Par[A] =
    es => cb => cb(a)

  /** A non-strict version of `unit` */
  def delay[A](a: => A): Par[A] =
    es => cb => cb(a)

  def fork[A](a: => Par[A]): Par[A] =
    es => cb => eval(es)(a(es)(cb))

  /** Helper function for constructing `Par` values out of calls to non-blocking
    * continuation-passing-style APIs. This will come in handy in Chapter 13.
    */
  def async[A](f: (A => Unit) => Unit): Par[A] =
    es => cb => f(cb)

  /** Helper function, for evaluating an action asynchronously, using the given
    * `ExecutorService`.
    */
  def eval(es: ExecutorService)(r: => Unit): Unit =
    es.submit(new Callable[Unit]:
      def call = r)

  def lazyUnit[A](a: => A): Par[A] =
    fork(unit(a))

  def asyncF[A, B](f: A => B): A => Par[B] =
    a => lazyUnit(f(a))

  def sequenceRight[A](as: List[Par[A]]): Par[List[A]] =
    as match
      case Nil    => unit(Nil)
      case h :: t => h.map2(fork(sequence(t)))(_ :: _)

  def sequenceBalanced[A](as: IndexedSeq[Par[A]]): Par[IndexedSeq[A]] =
    fork:
      if as.isEmpty then unit(Vector())
      else if as.length == 1 then map(as.head)(a => Vector(a))
      else
        val (l, r) = as.splitAt(as.length / 2)
        sequenceBalanced(l).map2(sequenceBalanced(r))(_ ++ _)

  def sequence[A](as: List[Par[A]]): Par[List[A]] =
    map(sequenceBalanced(as.toIndexedSeq))(_.toList)

  def parMap[A, B](as: List[A])(f: A => B): Par[List[B]] =
    sequence(as.map(asyncF(f)))

  def parMap[A, B](as: IndexedSeq[A])(f: A => B): Par[IndexedSeq[B]] =
    sequenceBalanced(as.map(asyncF(f)))

  def choice[A](cond: Par[Boolean])(t: Par[A], f: Par[A]): Par[A] =
    es =>
      cb =>
        cond(es) { b =>
          if b then eval(es) { t(es)(cb) } else eval(es)(f(es)(cb))
        }

  def choiceN[A](p: Par[Int])(ps: List[Par[A]]): Par[A] =
    es => cb => p(es)(i => eval(es)(ps(i % ps.length)(es)(cb)))

  def choiceViaChoiceN[A](cond: Par[Boolean])(t: Par[A], f: Par[A]): Par[A] =
    choiceN(cond.map(b => if b then 0 else 1))(List(t, f))

  def choiceMap[K, V](key: Par[K])(choices: Map[K, Par[V]]): Par[V] =
    es => cb => key(es)(k => eval(es)(choices(k)(es)(cb)))

  def choiceViaFlatMap[A](p: Par[Boolean])(f: Par[A], t: Par[A]): Par[A] =
    p.flatMap(b => if b then t else f)

  def choiceNViaFlatMap[A](p: Par[Int])(choices: List[Par[A]]): Par[A] =
    p.flatMap(i => choices(i))

  def join[A](ppa: Par[Par[A]]): Par[A] =
    es => cb => ppa(es)(pa => eval(es)(pa(es)(cb)))

  def joinViaFlatMap[A](ppa: Par[Par[A]]): Par[A] =
    ppa.flatMap(pa => pa)

  def flatMapViaJoin[A, B](pa: Par[A])(f: A => Par[B]): Par[B] =
    join(pa.map(f))
