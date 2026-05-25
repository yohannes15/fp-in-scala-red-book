package datastructures

import scala.collection.immutable.List

/** NOTES
  *
  * Passing around the `combineErrors` function is pretty inconvenient, and
  * we’ll see how to deal with such boilerplate in part 3 of this book
  *
  * We can avoid all the `combineErrors` here by explicitly passing
  * combineErrors by using a [[Monoid[E]]], covered in chapter 10, or the weaker
  * [[Semigroup[E]]]
  */
enum Validated[+E, +A]:
  case Valid(get: A)
  case Invalid(errors: E)

  def toEither: Either[E, A] = this match
    case Valid(a)    => Either.Right(a)
    case Invalid(es) => Either.Left(es)

  def map[B](f: A => B): Validated[E, B] = this match
    case Valid(a)    => Valid(f(a))
    case Invalid(es) => Invalid(es)

  def map2[EE >: E, B, C](
      b: Validated[EE, B]
  )(
      f: (A, B) => C
  )(
      combineErrors: (EE, EE) => EE
  ): Validated[EE, C] =
    (this, b) match
      case (Valid(aa), Valid(bb))     => Valid(f(aa, bb))
      case (Invalid(e), Valid(_))     => Invalid(e)
      case (Valid(_), Invalid(e))     => Invalid(e)
      case (Invalid(e1), Invalid(e2)) =>
        Invalid(combineErrors(e1, e2))

object Validated:
  def fromEither[E, A](e: Either[E, A]): Validated[E, A] =
    e match
      case Either.Right(a) => Valid(a)
      case Either.Left(es) => Invalid(es)

  def traverse[E, A, B](
      as: List[A],
      f: A => Validated[E, B],
      combineErrors: (E, E) => E
  ): Validated[E, List[B]] =
    as.foldRight(Valid(Nil)) {
      case (a, acc) => f(a).map2(acc)(_ :: _)(combineErrors)
    }

  def sequence[E, A](
      as: List[Validated[E, A]],
      combineErrors: (E, E) => E
  ): Validated[E, List[A]] =
    traverse(as, a => a, combineErrors)
