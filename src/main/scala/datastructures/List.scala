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

  def reverse[A](as: List[A]): List[A] =
    foldLeft(as, Nil, (acc, a) => Cons(a, acc))

  def foldRight[A, B](as: List[A], acc: B, f: (A, B) => B): B =
    as match
      case Nil         => acc
      case Cons(x, xs) => f(x, foldRight(xs, acc, f))

  @annotation.tailrec
  def foldLeft[A, B](as: List[A], acc: B, f: (B, A) => B): B =
    as match
      case Nil         => acc
      case Cons(h, tl) => foldLeft(tl, f(acc, h), f)

  /** Reverse the input list and then foldLeft with the result, flipping the
    * order of the parameters passed to the combining function
    */
  def foldRightViaFoldLeft[A, B](as: List[A], acc: B, f: (A, B) => B): B =
    foldLeft(reverse(as), acc, (acc, a) => f(a, acc))

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

  def append[A](xs: List[A], ys: List[A]): List[A] =
    foldRight(xs, ys, Cons(_, _))

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
