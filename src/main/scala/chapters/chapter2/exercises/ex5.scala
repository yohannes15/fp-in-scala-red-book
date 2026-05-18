package chapters.chapter2.exercises

/** feeds the output of one function to the input of another function */
@main def implementFunctionComposition: Unit =
  def compose[A, B, C](f: B => C, g: A => B): A => C =
    a => f(g(a))
