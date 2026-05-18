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

  def sum(ints: List[Int]): Int = ints match
    case Nil         => 0
    case Cons(x, xs) => x + sum(xs)

  def product(doubles: List[Double]): Double = doubles match
    case Nil          => 1.0
    case Cons(0.0, _) => 0.0
    case Cons(x, xs)  => x * product(xs)
