package parallel

import java.util.concurrent.*

/** Alternative representation of `Par` as a data type (enum) rather than an
  * opaque function. This allows pattern matching in combinators like `map` to
  * apply optimizations such as map fusion.
  *
  * [[ParRep[A]]] is a description of a parallel computation that gets turned
  * into a result when you call run. It is an inspectable tree of parallel
  * operations that, when interpreted, produces an A — a first-class program
  * that separates what to compute from how to run it.
  *
  * Compare with the opaque `Par` in [[Par]], where `y.map(g).map(f)` creates
  * two opaque closures. Here, `map` pattern matches and composes functions:
  * {{{
  *   y.map(g).map(f)  =>  Map(Map(y, g), f)         // without fusion
  *   y.map(g).map(f)  =>  Map(y, g andThen f)       // with fusion
  * }}}
  */
enum ParRep[A]:
  case Unit(a: A)
  case Map[A, B](source: ParRep[A], f: A => B) extends ParRep[B]
  case Map2[A, B, C](pa: ParRep[A], pb: ParRep[B], f: (A, B) => C)
      extends ParRep[C]
  case Fork[A](source: () => ParRep[A]) extends ParRep[A]
  case Map2Timeouts[A, B, C](pa: ParRep[A], pb: ParRep[B], f: (A, B) => C)
      extends ParRep[C]
  case ChoiceN[A](p: ParRep[Int], ps: List[Par[A]]) extends ParRep[A]

  /** map with fusion: if this is already a `Map`, compose functions instead of
    * nesting another layer.
    */
  def map[B](f: A => B): ParRep[B] = this match
    case Map(source, g) => Map(source, g andThen f) // FUSE!
    case other          => Map(other, f)

  /** Combines the results of two ParRep computations with a binary function.
    * Constructs a Map2 node — the actual evaluation happens in `run`.
    */
  def map2[B, C](pb: ParRep[B])(f: (A, B) => C): ParRep[C] =
    Map2(this, pb, f)

  /** Like `map2`, but respects timeouts via `Map2Future` in `run`. */
  def map2Timeouts[B, C](pb: ParRep[B])(f: (A, B) => C): ParRep[C] =
    Map2Timeouts(this, pb, f)

  def map3[B, C, D](pb: ParRep[B], pc: ParRep[C])(
      f: (A, B, C) => D
  ): ParRep[D] =
    val combined: ParRep[C => D] = this.map2(pb) { (a, b) => (c: C) =>
      f(a, b, c)
    }
    combined.map2(pc)((g, c) => g(c))

  /** Interprets the ParRep description, executing it via the given
    * ExecutorService. Returns a Future to match `Par.run`.
    */
  def run(es: ExecutorService): Future[A] = this match
    case Unit(a)        => UnitFuture(a)
    case Map(source, f) => UnitFuture(f(source.run(es).get))
    case Fork(src)      => es.submit(new Callable[A]:
        def call = src().run(es).get)
    case Map2(pa, pb, f) =>
      UnitFuture(f(pa.run(es).get, pb.run(es).get))
    case Map2Timeouts(pa, pb, f) =>
      Map2Future(pa.run(es), pb.run(es), f)
    case ChoiceN(p, ps) =>
      val index = p.run(es).get % ps.size
      ps(index).run(es)

  /** A data type separates description from execution. The tree of operations
    * can be inspected, rewritten, and optimized before `run` touches an
    * `ExecutorService`. You could write a second interpreter that logs every
    * step without changing a line of user code:
    */
  def traceRun(es: ExecutorService): Future[A] = this match
    case Unit(a)     => println(s"Unit($a)"); UnitFuture(a)
    case Map(src, f) =>
      println("Map"); UnitFuture(f(src.traceRun(es).get))
    case Fork(src) =>
      println("Fork → submit"); src().traceRun(es)
    case Map2(pa, pb, f) =>
      println("Map2"); UnitFuture(f(pa.run(es).get, pb.run(es).get))
    case Map2Timeouts(pa, pb, f) =>
      println("Map2Timeouts"); Map2Future(pa.run(es), pb.run(es), f)
    case ChoiceN(p, ps) =>
      println("ChoiceN"); val idx = p.run(es).get % ps.size; ps(idx).run(es)

object ParRep:
  /** Promotes a constant value to a parallel computation. */
  def unit[A](a: A): ParRep[A] = Unit(a)

  /** Marks a computation for concurrent evaluation — the evaluation won't occur
    * until forced by `run`.
    */
  def fork[A](a: => ParRep[A]): ParRep[A] = Fork(() => a)

  /** Wraps a by-name value `a` into a `ParRep` that evaluates it in a separate
    * thread via `fork(unit(a))`.
    */
  def lazyUnit[A](a: => A): ParRep[A] = fork(unit(a))

  /** Converts any `A => B` to one that evaluates its result asynchronously. */
  def asyncF[A, B](f: A => B): A => ParRep[B] =
    a => lazyUnit(f(a))

  def sequence[A](ps: List[ParRep[A]]): ParRep[List[A]] =
    sequenceBalanced(ps.toIndexedSeq).map(_.toList)

  def sequenceBalanced[A](
      pas: IndexedSeq[ParRep[A]]
  ): ParRep[IndexedSeq[A]] =
    if pas.isEmpty then unit(IndexedSeq.empty)
    else if pas.size == 1 then pas.head.map(a => IndexedSeq(a))
    else
      val (l, r) = pas.splitAt(pas.size / 2)
      sequenceBalanced(l).map2(sequenceBalanced(r))(_ ++ _)

  def parMap[A, B](as: List[A])(f: A => B): ParRep[List[B]] =
    fork {
      val fbs: List[ParRep[B]] = as.map(asyncF(f))
      sequence(fbs)
    }

  def parFilter[A](as: List[A])(f: A => Boolean): ParRep[List[A]] =
    fork {
      val pars = as.map(asyncF(a => if f(a) then List(a) else Nil))
      sequence(pars).map(_.flatten)
    }

  def parFold[A](ints: IndexedSeq[A])(z: A)(f: (A, A) => A): ParRep[A] =
    parFoldMap(ints)(z)(identity[A])(f)

  /** generalizing the map-transform-combine pattern */
  def parFoldMap[A, B](as: IndexedSeq[A])(z: B)(
      f: A => B
  )(g: (B, B) => B): ParRep[B] =
    if as.isEmpty then ParRep.unit(z)
    else if as.size == 1 then ParRep.unit(f(as.head))
    else
      val (l, r) = as.splitAt(as.size / 2)
      val parL = ParRep.fork(parFoldMap(l)(z)(f)(g))
      val parR = ParRep.fork(parFoldMap(r)(z)(f)(g))
      parL.map2(parR)(g)

  def max(ints: IndexedSeq[Int]): ParRep[Int] =
    parFold(ints)(Int.MinValue)(_ max _)

  def sum(ints: IndexedSeq[Int]): ParRep[Int] =
    parFold(ints)(0)(_ + _)

object ParRepExamples:
  def sum(ints: IndexedSeq[Int]): ParRep[Int] =
    if ints.size <= 1 then ParRep.unit(ints.headOption.getOrElse(0))
    else
      val (l, r) = ints.splitAt(ints.size / 2)
      val parL = ParRep.fork(sum(l))
      val parR = ParRep.fork(sum(r))
      parL.map2(parR)(_ + _)

  def sortPar(parList: ParRep[List[Int]]): ParRep[List[Int]] =
    parList.map(_.sorted)

  def totalNumberOfWords(paragraphs: List[String]): ParRep[Int] =
    ParRep.parFoldMap(
      paragraphs.toIndexedSeq
    )(0)(_.split("\\s+").size)(_ + _)
