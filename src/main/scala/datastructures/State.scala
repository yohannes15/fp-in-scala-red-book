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
    // states.foldRight(unit(Nil)) { (s, acc) => s.map2(acc)(_ :: _) }
    traverse(states)(s => s)

  def traverse[S, A, B](as: List[A])(f: A => State[S, B]): State[S, List[B]] =
    as.foldRight(unit(Nil))((a, acc) => f(a).map2(acc)(_ :: _))

  /** simply passes the incoming state along and returns it as the value */
  def get[S]: State[S, S] = s => (s, s)

  /** resulting action ignores the incoming state, replaces it with the new
    * state, and returns () instead of a meaningful value
    */
  def set[S](s: S): State[S, Unit] = _ => ((), s)

  def modify[S](f: S => S): State[S, Unit] =
    for
      s <- get // Gets the current state and assigns it to s
      _ <- set(f(s)) // Sets the new state to f applied to s
    yield ()
