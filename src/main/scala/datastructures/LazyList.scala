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
