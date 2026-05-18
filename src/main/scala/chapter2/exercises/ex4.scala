package chapter2.exercises

/** Reverse a curried function */
@main def implementUncurry: Unit =

  def uncurry[A, B, C](f: A => B => C): (A, B) => C =
    (a, b) => f(a)(b)
