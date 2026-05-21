package chapter3.exercises

import datastructures.List.*
import datastructures.List

@main def resultOfBelowExpressionIsWhat: Unit =
  val result = List(1, 2, 3, 4, 5) match
    // This doesn't match because 2 -> 4 is invalid
    case Cons(x, Cons(2, Cons(4, _))) => x
    // the literal List isn't Nil this invalid
    case Nil => 42
    // x = 1, y = 2, then 3 -> 4 -> _. This is valid
    // RESULT OF EXPRESSION THEN IS 3
    case Cons(x, Cons(y, Cons(3, Cons(4, _)))) => x + y
    // order matters even if this matches above is the result
    case Cons(h, t) => h + sum(t)
    // Doesn't reach here as well
    case null => 101

  /* ANSWER IS 3. The x + y path*/
  print(result)
