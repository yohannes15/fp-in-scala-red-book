package datastructures

import scala.Option
import scala.annotation.tailrec
import scala.collection.immutable.List

enum LazyList[+A]:
  case Empty
  /* nonempty lazy list consists of nonstrict head and tail.*/
  case Cons(h: () => A, t: () => LazyList[A])

  import LazyList.*

  def toList: List[A] = this match
    case Empty      => Nil
    case Cons(h, t) => h() :: t().toList

  /** If f chooses not to evaluate its second parameter, the traversal is
    * terminated early; we can see this by using foldRight to implement exists
    *
    * Since foldRight can terminate the traversal early, we can reuse it to
    * implement exists, which we can’t do with a strict version of foldRight;
    * we’d have to write a specialized recursive exists function to handle early
    * termination. Laziness makes our code more reusable.
    */
  def foldRight[B](acc: => B)(f: (A, => B) => B): B =
    this match
      case Cons(h, t) => f(h(), t().foldRight(acc)(f))
      case _          => acc

  def takeWhile(p: A => Boolean): LazyList[A] =
    foldRight(empty)((a, b) => if p(a) then cons(a, b) else empty)

  def headOption: Option[A] =
    foldRight(None)((a, _) => Some(a))

  /** Here [[b]] is the unevaluated recursive step that folds the tail of the
    * lazy list. If [[p(a)]] returns true, [[b]] will never be evaluated, and
    * the computation will terminate early.
    *
    * NOTE: This definition of exists, though illustrative, isn’t stack safe if
    * the lazy list is large and all elements test false.
    */
  def exists(p: A => Boolean): Boolean =
    foldRight(false)((a, b) => p(a) || b)

  def forAll(p: A => Boolean): Boolean =
    foldRight(true)((a, b) => p(a) && b)

  def map[B](f: A => B): LazyList[B] =
    foldRight(empty)((a, acc) => cons(f(a), acc))

  def flatMap[B](f: A => LazyList[B]): LazyList[B] =
    foldRight(empty)((a, acc) => f(a).append(acc))

  def filter(p: A => Boolean): LazyList[A] =
    foldRight(empty)((a, acc) => if p(a) then cons(a, acc) else acc)

  def find(p: A => Boolean): Option[A] =
    filter(p).headOption

  def append[A2 >: A](that: => LazyList[A2]): LazyList[A2] =
    foldRight(that)((a, acc) => cons(a, acc))

  /** Stack safe because the recursive call is passed as cons's by-name tail.
    * take returns after building one Cons; the rest is evaluated only when
    * forced. the recursive call is suspended until the tail of the returned
    * Cons is forced, and hence this is stack safe.
    *
    * [[t().take(n - 1)]] is not evaluated immediately. take builds one Cons and
    * returns. The recursive call is deferred until the tail of the resulting
    * lazy list is forced. Thus stack safety here comes from laziness, not tail
    * recursion.
    */
  def take(n: Int): LazyList[A] = this match
    case Cons(h, t) if n > 1  => cons(h(), t().take(n - 1))
    case Cons(h, t) if n == 1 => cons(h(), empty)
    case _                    => empty

  /** Implements `take` with `foldRight` by making the fold produce a function
    * `Int => LazyList[A]`.
    *
    * The extra `Int` argument is the remaining number of elements to keep.
    * `foldRight` gives us the current element and a lazy folded tail; wrapping
    * the result in a function lets each step decide whether to include the
    * current element, stop at exactly one element, or return `empty`.
    *
    * The initial `n <= 0` check avoids forcing the head just to build the
    * folded function, preserving the same behavior as `take(0)`.
    */
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

  @tailrec
  final def drop(n: Int): LazyList[A] = this match
    case Cons(h, t) if n > 0 => t().drop(n - 1)
    case _                   => this

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

  def continually[A](a: A): LazyList[A] = cons(a, continually(a))
  def from(n: Int): LazyList[Int] = cons(n, from(n + 1))

  /** corecursive general LazyList-building function. It takes an initial state
    * and a function. The function evaluates the state and returns an Option
    * containing the current value and the next state. As long as it returns
    * Some, the structure continues to grow stack safe because Cons lazily
    * evaluates its arguments.
    */
  def unfold[A, S](state: S)(f: S => Option[(A, S)]): LazyList[A] =
    f(state) match
      case Some((a, s)) => cons(a, unfold(s)(f))
      case _            => empty

  val ones: LazyList[Int] = cons(1, ones)

  // 0, 1, 1, 2, 3, 5, 8, 13, ...
  def fibs: LazyList[Int] =
    /** a recursive auxiliary function that tracks the current and next
      * Fibonacci numbers. When it’s evaluated, it returns a lazy list with the
      * current Fibonacci number as the head and a recursive call as the tail,
      * shifting the next number into the current position and computing a new
      * next by summing current and next
      */
    def go(curr: Int, nxt: Int): LazyList[Int] =
      cons(curr, go(nxt, curr + nxt))
    go(0, 1)
