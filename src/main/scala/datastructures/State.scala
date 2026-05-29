package datastructures

import scala.List

opaque type State[S, +A] = S => (A, S)

object State:
  extension [S, A](underlying: State[S, A])
    def run(s: S): (A, S) = underlying(s)

    def map[B](f: A => B): State[S, B] =
      flatMap(a => unit(f(a)))

    def map2[B, C](sb: State[S, B])(f: (A, B) => C): State[S, C] =
      // underlying.flatMap(a => sb.map(b => f(a, b)))
      for
        a <- underlying
        b <- sb
      yield f(a, b)

    def flatMap[B](f: A => State[S, B]): State[S, B] =
      s =>
        val (a, s1) = run(s)
        f(a)(s)

  def apply[S, A](f: S => (A, S)): State[S, A] = f
  def unit[S, A](a: A): State[S, A] = s => (a, s)

  def sequence[S, A](states: List[State[S, A]]): State[S, List[A]] =
    states.foldRight(unit(Nil)) { (s, acc) => s.map2(acc)(_ :: _) }
