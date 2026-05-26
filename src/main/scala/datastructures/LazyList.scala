package datastructures

import scala.Option
import scala.annotation.tailrec
import scala.collection.immutable.List

enum LazyList[+A]:
  import LazyList.*
  case Empty
  /* nonempty lazy list consists of nonstrict head and tail.*/
  case Cons(h: () => A, t: () => LazyList[A])

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

  @tailrec
  final def drop(n: Int): LazyList[A] = this match
    case Cons(h, t) if n > 0 => t().drop(n - 1)
    case _                   => this

  /** Stack safety again comes from laziness not tail recursion. */
  def takeWhile(p: A => Boolean): LazyList[A] = this match
    case Cons(h, t) if p(h()) => cons(h(), t().takeWhile(p))
    case _                    => empty

  /** optionall extract the head of a LazyList */
  def headOption: Option[A] = this match
    case Empty      => None
    case Cons(h, _) => Some(h()) // forcing of the the h thunk using h()

  /** Here [[b]] is the unevaluated recursive step that folds the tail of the
    * lazy list. If [[p(a)]] returns true, [[b]] will never be evaluated, and
    * the computation will terminate early.
    *
    * NOTE: This definition of exists, though illustrative, isn’t stack safe if
    * the lazy list is large and all elements test false.
    */
  def exists(p: A => Boolean): Boolean =
    this.foldRight(false)((a, b) => p(a) || b)

  def forAll(p: A => Boolean): Boolean =
    ???

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
