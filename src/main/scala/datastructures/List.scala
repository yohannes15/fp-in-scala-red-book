package datastructures

/** A singly linked list.
  *
  * NOTE: List is parameterized on a type, A, as a result the data constructors
  * and methods on the companion are polymorphic functions that can be
  * instantiated with different types for A.
  */
enum List[+A]:
  /** NOTE: Nil extends List[Nothing]; we could have explicitly stated this
    *   - `case Nil extends List[Nothing]`
    *
    * but since we didn’t, Scala inferred that relationship. Nothing is a
    * subtype of all types, which means in conjunction with the variance
    * annotation.
    *
    * NOTE: Nil can be considered a List[Int], a List[Double], and so on,
    * exactly as we want.
    */
  case Nil
  case Cons(head: A, tail: List[A]) extends List[A]

/** Note that these are recursive defintions, which are common when writing
  * functions that operate over recursive data types like `List` (which refers
  * to itself recursively in its `Cons` data constructor).
  */
object List:
  def apply[A](as: A*): List[A] =
    if as.isEmpty then Nil
    else Cons(as.head, apply(as.tail*))

  def sum(ints: List[Int]): Int =
    foldLeft(ints, 0, _ + _)

  def product(doubles: List[Double]): Double =
    foldLeft(doubles, 0, _ * _)

  def length[A](as: List[A]): Int =
    foldLeft(as, 0, (acc, _) => acc + 1)

  def foldRight[A, B](as: List[A], acc: B, f: (A, B) => B): B =
    as match
      case Nil         => acc
      case Cons(x, xs) => f(x, foldRight(xs, acc, f))

  @annotation.tailrec
  def foldLeft[A, B](as: List[A], acc: B, f: (B, A) => B): B =
    as match
      case Nil         => acc
      case Cons(h, tl) => foldLeft(tl, f(acc, h), f)

  def tail[A](as: List[A]): List[A] = as match
    case Cons(_, tl) => tl
    case Nil => throw new UnsupportedOperationException("tail of empty list")

  def setHead[A](as: List[A], h: A): List[A] = as match
    case Nil =>
      throw new UnsupportedOperationException("set head of empty list")
    case Cons(_, tl) => Cons(h, tl)

  def drop[A](as: List[A], n: Int): List[A] =
    if n <= 0 then as
    else
      as match
        case Nil           => as
        case Cons(_, tail) => drop(tail, n - 1)

  def dropWhile[A](as: List[A], f: A => Boolean): List[A] = as match
    case Cons(h, tl) if f(h) => dropWhile(tl, f)
    case _                   => as

  /** Note that this definition only copies values until the first list is
    * exhausted, so its runtime and memory usage are determined only by the
    * length of a1. The remaining list then just points to a2. If we were to
    * implement this same function for two arrays, we’d be forced to copy all
    * the elements in both arrays into the result. In this case, the immutable
    * linked list is much more efficient than an array!
    */
  def append[A](a1: List[A], a2: List[A]): List[A] =
    a1 match
      case Nil        => a2
      case Cons(h, t) => Cons(h, append(t, a2))

  /** returns the list but with the last element removed. The runtime of init is
    * proportional to the length of the list. Furthermore, we have to build up a
    * copy of the entire list, as there's no structural sharing between the
    * initial list and the result of init. Finally this implementation uses a
    * stack frame for each element of the list, leading to potential stack
    * overflow errors.
    */
  def init[A](as: List[A]): List[A] = as match
    case Nil => throw new UnsupportedOperationException("init of empty list")
    case Cons(_, Nil) => Nil
    case Cons(h, tl)  => Cons(h, init(tl))
