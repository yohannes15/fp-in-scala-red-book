import datastructures.List
import List.*

/** ## Exercise 3.16
  *
  * Write a function that transforms a list of integers by adding 1 to each
  * element (that is, given a list of integers, it returns a new list of
  * integers where each value is one more than the corresponding value in the
  * original list).
  */
@main def ex3_16: Unit =
  /** We use foldRight, so we can build the result list in the correct order
    * while using Cons as our combining function. Before we create a Cons value,
    * we increment the integer passed to the combining function.
    */
  def incrementEach(as: List[Int]): List[Int] =
    foldRight(as, Nil, (a, acc) => Cons(a + 1, acc))
