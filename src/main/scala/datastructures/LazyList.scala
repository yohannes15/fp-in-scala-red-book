package datastructures

import scala.Option
import scala.annotation.tailrec
import scala.collection.immutable.List

enum LazyList[+A]:
  case Empty
  /* nonempty lazy list consists of nonstrict head and tail.*/
  case Cons(h: () => A, t: () => LazyList[A])

  import LazyList.*

  def toListRecursive: List[A] = this match
    case Empty      => Nil
    case Cons(h, t) => h() :: t().toList

  def toList: List[A] =
    @tailrec
    def go(l: LazyList[A], acc: List[A]): List[A] = l match
      case Cons(h, t) => go(t(), h() :: acc)
      case Empty      => acc
    go(this, Nil).reverse

  def foldRight[B](acc: => B)(f: (A, => B) => B): B =
    this match
      case Cons(h, t) => f(h(), t().foldRight(acc)(f))
      case _          => acc

  def headOption: Option[A] =
    foldRight(None)((a, _) => Some(a))

  def exists(p: A => Boolean): Boolean =
    foldRight(false)((a, b) => p(a) || b)

  def forAll(p: A => Boolean): Boolean =
    foldRight(true)((a, b) => p(a) && b)

  def map[B](f: A => B): LazyList[B] =
    foldRight(empty)((a, acc) => cons(f(a), acc))

  def mapViaUnfold[B](f: A => B): LazyList[B] =
    unfold(this) {
      case Cons(h, t) => Some(f(h()), t())
      case _          => None
    }

  def flatMap[B](f: A => LazyList[B]): LazyList[B] =
    foldRight(empty)((a, acc) => f(a).append(acc))

  def filter(p: A => Boolean): LazyList[A] =
    foldRight(empty)((a, acc) => if p(a) then cons(a, acc) else acc)

  def find(p: A => Boolean): Option[A] =
    filter(p).headOption

  def append[A2 >: A](that: => LazyList[A2]): LazyList[A2] =
    foldRight(that)((a, acc) => cons(a, acc))

  def take(n: Int): LazyList[A] = this match
    case Cons(h, t) if n > 1  => cons(h(), t().take(n - 1))
    case Cons(h, t) if n == 1 => cons(h(), empty)
    case _                    => empty

  def takeViaUnfold(n: Int): LazyList[A] =
    unfold((this, n)) {
      case (Cons(h, t), 1)          => Some((h(), (empty, 0)))
      case (Cons(h, t), n) if n > 1 => Some((h(), (t(), n - 1)))
      case _                        => None
    }

  def takeUsingFoldRight(n: Int): LazyList[A] =
    if n <= 0 then empty
    else
      /** Since foldRight doesn’t naturally track an index/count, returning a
        * function Int => LazyList[A] is the standard trick for threading the
        * remaining count through the fold.
        */
      val takeFn: Int => LazyList[A] =
        foldRight((i: Int) => Empty) {
          (a, nextFunc) => (i: Int) =>
            if i > 1 then cons(a, nextFunc(i - 1))
            else if i == 1 then cons(a, empty)
            else empty
        }
      takeFn(n)

  def takeWhile(p: A => Boolean): LazyList[A] =
    foldRight(empty)((a, b) => if p(a) then cons(a, b) else empty)

  def takeWhileViaUnfold(p: A => Boolean): LazyList[A] =
    unfold(this) {
      case Cons(h, t) if p(h()) => Some((h(), t()))
      case _                    => None
    }

  @tailrec
  final def drop(n: Int): LazyList[A] = this match
    case Cons(h, t) if n > 0 => t().drop(n - 1)
    case _                   => this

  def zipWith[B, C](that: LazyList[B])(f: (A, B) => C): LazyList[C] =
    unfold((this, that)) {
      case (Cons(h, t), Cons(h2, t2)) => Some((f(h(), h2()), (t(), t2())))
      case _                          => None
    }

  def zip[B](that: LazyList[B]): LazyList[(A, B)] =
    zipWith(that)((_, _))

  /** continue traversal as long as either lazy list has more elements */
  def zipAll[B](that: LazyList[B]): LazyList[(Option[A], Option[B])] =
    unfold((this, that)) {
      case (Empty, Empty)             => None
      case (Cons(h, t), Empty)        => Some((Some(h()), None), (t(), Empty))
      case (Empty, Cons(h2, t2))      => Some((None, Some(h2())), (Empty, t2()))
      case (Cons(h, t), Cons(h2, t2)) =>
        Some((Some(h()), Some(h2())), (t(), t2()))
    }

  def zipAll2[B](s2: LazyList[B]): LazyList[(Option[A], Option[B])] =
    zipWithAll(s2)((_, _))

  def zipWithAll[B, C](that: LazyList[B])(f: (Option[A], Option[B]) => C)
      : LazyList[C] =
    LazyList.unfold((this, that)) {
      case (Empty, Empty)      => None
      case (Cons(h, t), Empty) => Some(f(Some(h()), None), (t(), Empty))
      case (Empty, Cons(h, t)) => Some(f(None, Some(h())), (empty, t()))
      case (Cons(h1, t1), Cons(h2, t2)) =>
        Some(f(Some(h1()), Some(h2())), (t1(), t2()))
    }

object LazyList:
  /** smart constructor for creating nonempty LazyList of particular type */
  def cons[A](hd: => A, tl: => LazyList[A]): LazyList[A] =
    // cache the head and tail as lazy values to avoid repeated evaluation
    lazy val head = hd
    lazy val tail = tl
    Cons(() => head, () => tail)

  /** smart constructor for creating an empty LazyList of particular type */
  def empty[A]: LazyList[A] = Empty

  /** convenient method for constructing LazyList from varargs. */
  def apply[A](as: A*): LazyList[A] =
    if as.isEmpty then empty
    else cons(as.head, apply(as.tail*))

  def unfold[A, S](state: S)(f: S => Option[(A, S)]): LazyList[A] =
    f(state) match
      case Some((a, s)) => cons(a, unfold(s)(f))
      case _            => empty

  def continually[A](a: A): LazyList[A] = unfold(())(_ => Some((a, ())))
  def continuallyRecursive[A](a: A): LazyList[A] =
    lazy val single: LazyList[A] = cons(a, single)
    single

  def from(n: Int): LazyList[Int] = unfold(n)(n => Some((n, n + 1)))
  def fromRecursive(n: Int): LazyList[Int] = cons(n, from(n + 1))

  val ones: LazyList[Int] = unfold(())(_ => Some((1, ())))
  val onesRecursive: LazyList[Int] = cons(1, ones)

  // 0, 1, 1, 2, 3, 5, 8, 13, ...
  val fibs: LazyList[Int] =
    unfold((0, 1))((curr, nxt) => Some((curr, (nxt, curr + nxt))))
  val fibsRecursive: LazyList[Int] =
    def go(curr: Int, nxt: Int): LazyList[Int] =
      cons(curr, go(nxt, curr + nxt))
    go(0, 1)
