package datastructures

import scala.collection.immutable.List

enum Validated[+E, +A]:
  case Valid(get: A)
  case Invalid(errors: List[E])

  def toEither: Either[List[E], A] = this match
    case Valid(a)    => Either.Right(a)
    case Invalid(es) => Either.Left(es)

  def map[B](f: A => B): Validated[E, B] = this match
    case Valid(a)    => Valid(f(a))
    case Invalid(es) => Invalid(es)

  def map2[EE >: E, B, C](
      b: Validated[EE, B]
  )(f: (A, B) => C): Validated[EE, C] =
    (this, b) match
      case (Valid(aa), Valid(bb))       => Valid(f(aa, bb))
      case (Invalid(es), Valid(_))      => Invalid(es)
      case (Valid(_), Invalid(es))      => Invalid(es)
      case (Invalid(es1), Invalid(es2)) => Invalid(es1 ++ es2)

object Validated:
  def fromEither[E, A](e: Either[List[E], A]): Validated[E, A] =
    e match
      case Either.Right(a) => Valid(a)
      case Either.Left(es) => Invalid(es)

  def traverse[E, A, B](
      as: List[A],
      f: A => Validated[E, B]
  ): Validated[E, List[B]] =
    as.foldRight(Valid(Nil)) {
      case (a, acc) => f(a).map2(acc)(_ :: _)
    }

  def sequence[E, A](as: List[Validated[E, A]]): Validated[E, List[A]] =
    traverse(as, a => a)
