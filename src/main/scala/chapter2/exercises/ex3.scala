package chapter2.exercises

/** Let’s look at another example, currying, which converts a function f of two
  * arguments into a function of one argument that partially applies f. Here
  * again there’s only one implementation that compiles. Write this
  * implementation:
  *
  * Note the type A => (B => C) can be read as a function that takes an A and
  * returns a new function from B to C. Also as A => B => C
  */
@main def implementCurry: Unit =
  def curry[A, B, C](f: (A, B) => C): A => (B => C) =
    a => (b => f(a, b))

  val curriedGreaterThanFunc = curry((x: Int, y: Int) => x > y)
  // curriedGreaterThanFunc: Int => Int => Boolean

  val res = curriedGreaterThanFunc(2)(3)
  println(s"Res of 2 > 3 using curried func: $res")
