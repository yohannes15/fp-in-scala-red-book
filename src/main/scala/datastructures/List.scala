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

object List:
  def apply[A](as: A*): List[A] =
    if as.isEmpty then Nil
    else Cons(as.head, apply(as.tail*))
