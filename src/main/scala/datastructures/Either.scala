package datastrucutres

import scala.util.control.NonFatal

/** Notes
  *
  * When `flatMap` over the right side, we must promote the left type parameter
  * to some supertype to satisfy the +E variance annotation. It is similar for
  * orElse.
  */
enum Either[+E, +A]:
  case Left(value: E)
  case Right(value: A)

  def map[B](f: A => B): Either[E, B] = this match
    case Right(a) => Right(f(a))
    case Left(e)  => Left(e)

  def flatMap[EE >: E, B](f: A => Either[EE, B]): Either[EE, B] = this match
    case Right(v) => f(v)
    case Left(e)  => Left(e)

  def getOrElse[B >: A](or: => B): B = this match
    case Right(a) => a
    case _        => or

  def orElse[EE >: E, B >: A](b: => Either[EE, B]): Either[EE, B] = this match
    case Right(v) => Right(v)
    case Left(_)  => b

  def map2[EE >: E, B, C](that: Either[EE, B])(f: (A, B) => C): Either[EE, C] =
    // this.flatMap(a => that.map(b => f(a, b)))
    for
      a <- this
      b <- that
    yield f(a, b)

  def sequence[E, A](as: List[Either[E, A]]): Either[E, List[A]] =
    // as.foldRight(Right(Nil)) {
    //   case (a, acc) => a.map2(acc)(_ :: _)
    // }
    traverse(as)(a => a)

  def traverse[E, A, B](as: List[A])(f: A => Either[E, B]): Either[E, List[B]] =
    as.foldRight(Right(Nil)) {
      case (a, acc) => f(a).map2(acc)(_ :: _)
    }

object Either:
  def catchNonFatal[A](a: => A): Either[Throwable, A] =
    try Right(a)
    catch case NonFatal(t) => Left(t)
