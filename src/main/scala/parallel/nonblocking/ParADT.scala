package parallel.nonblocking

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference

/** Nonblocking `Par` encoded as an algebraic data type. Programs are
  * represented as data (`enum`) rather than as functions. Interpretation
  * happens in `step`, which pattern-matches on the enum.
  */
enum ParADT[+A]:
  /** Primitive constructors */
  case Pure(a: A)
  case Delay(a: () => A)
  case Fork(pa: () => ParADT[A])
  case Async(f: (A => Unit) => Unit)
  case FlatMap[A, B](source: ParADT[A], f: A => ParADT[B]) extends ParADT[B]
  case Map2[A, B, C](pa: ParADT[A], pb: ParADT[B], f: (A, B) => C)
      extends ParADT[C]

  /** Derived combinators */
  def map[B](f: A => B): ParADT[B] = FlatMap(this, a => ParADT.Pure(f(a)))

  def flatMap[B](f: A => ParADT[B]): ParADT[B] = FlatMap(this, f)

  def map2[B, C](pb: ParADT[B])(f: (A, B) => C): ParADT[C] =
    ParADT.Map2(this, pb, f)

  def zip[B](pb: ParADT[B]): ParADT[(A, B)] = Map2(this, pb, (_, _))

  /** Block until the result is available */
  def run(es: ExecutorService): A =
    val ref = new AtomicReference[A]
    val latch = new CountDownLatch(1)
    ParADT.step(es)(this)(a =>
      ref.set(a); latch.countDown
    )
    latch.await
    ref.get

object ParADT:
  /** Interpreter: pattern matches on the enum and executes */
  private def step[A](es: ExecutorService)(pa: ParADT[A])(cb: A => Unit): Unit =
    pa match
      case ParADT.Pure(a)            => cb(a)
      case ParADT.Delay(a)           => cb(a())
      case ParADT.Fork(pa)           => eval(es)(step(es)(pa())(cb))
      case ParADT.Async(f)           => f(cb)
      case ParADT.FlatMap(src, f)    => flatMapStep(es)(src, f, cb)
      case m2 @ ParADT.Map2(_, _, _) => map2Step(es)(m2, cb)

  /** Helper to avoid existential type issues with GADT pattern matching */
  private def flatMapStep[X, A](es: ExecutorService)(
      source: ParADT[X],
      f: X => ParADT[A],
      cb: A => Unit
  ): Unit =
    eval(es):
      step(es)(source)(a => step(es)(f(a))(cb))

  /** Helper to avoid existential type issues with GADT pattern matching */
  private def map2Step[X, Y, A](es: ExecutorService)(
      m2: ParADT.Map2[X, Y, A],
      cb: A => Unit
  ): Unit =
    var ar: Option[X] = None
    var br: Option[Y] = None
    val combiner = Actor[Either[X, Y]](es):
      case Left(a) =>
        if br.isDefined then eval(es)(cb(m2.f(a, br.get)))
        else ar = Some(a)
      case Right(b) =>
        if ar.isDefined then eval(es)(cb(m2.f(ar.get, b)))
        else br = Some(b)
    step(es)(m2.pa)(a => combiner ! Left(a))
    step(es)(m2.pb)(b => combiner ! Right(b))

  /** Smart constructors */
  def unit[A](a: A): ParADT[A] = ParADT.Pure(a)

  def delay[A](a: => A): ParADT[A] = ParADT.Delay(() => a)

  def fork[A](pa: => ParADT[A]): ParADT[A] = ParADT.Fork(() => pa)

  def async[A](f: (A => Unit) => Unit): ParADT[A] = ParADT.Async(f)

  /** Helper: submit an action to the thread pool */
  def eval(es: ExecutorService)(r: => Unit): Unit =
    es.submit(new Callable[Unit]:
      def call = r)

  def lazyUnit[A](a: => A): ParADT[A] = fork(unit(a))

  def asyncF[A, B](f: A => B): A => ParADT[B] =
    a => lazyUnit(f(a))

  /** Sequencing */
  def sequenceRight[A](as: List[ParADT[A]]): ParADT[List[A]] =
    as match
      case Nil    => unit(Nil)
      case h :: t => h.map2(fork(sequence(t)))(_ :: _)

  def sequenceBalanced[A](as: IndexedSeq[ParADT[A]]): ParADT[IndexedSeq[A]] =
    fork:
      if as.isEmpty then unit(Vector())
      else if as.length == 1 then as.head.map(a => Vector(a))
      else
        val (l, r) = as.splitAt(as.length / 2)
        sequenceBalanced(l).map2(sequenceBalanced(r))(_ ++ _)

  def sequence[A](as: List[ParADT[A]]): ParADT[List[A]] =
    sequenceBalanced(as.toIndexedSeq).map(_.toList)

  def parMap[A, B](as: List[A])(f: A => B): ParADT[List[B]] =
    sequence(as.map(asyncF(f)))

  def parMap[A, B](as: IndexedSeq[A])(f: A => B): ParADT[IndexedSeq[B]] =
    sequenceBalanced(as.map(asyncF(f)))

  /** Choice combinators */
  def choice[A](cond: ParADT[Boolean])(t: ParADT[A], f: ParADT[A]): ParADT[A] =
    cond.flatMap(b => if b then t else f)

  def choiceN[A](n: ParADT[Int])(choices: List[ParADT[A]]): ParADT[A] =
    n.flatMap(i => choices(i))

  def choiceViaChoiceN[A](cond: ParADT[Boolean])(
      t: ParADT[A],
      f: ParADT[A]
  ): ParADT[A] =
    choiceN(cond.map(b => if b then 0 else 1))(List(t, f))

  def choiceMap[K, V](key: ParADT[K])(choices: Map[K, ParADT[V]]): ParADT[V] =
    key.flatMap(k => choices(k))

  def choiceViaFlatMap[A](p: ParADT[Boolean])(
      f: ParADT[A],
      t: ParADT[A]
  ): ParADT[A] =
    p.flatMap(b => if b then t else f)

  def choiceNViaFlatMap[A](p: ParADT[Int])(choices: List[ParADT[A]])
      : ParADT[A] =
    p.flatMap(i => choices(i))

  /** Join - flatten nested Par */
  def join[A](ppa: ParADT[ParADT[A]]): ParADT[A] =
    ppa.flatMap(identity)

  def joinViaFlatMap[A](ppa: ParADT[ParADT[A]]): ParADT[A] =
    ppa.flatMap(pa => pa)

  def flatMapViaJoin[A, B](pa: ParADT[A])(f: A => ParADT[B]): ParADT[B] =
    join(pa.map(f))
