import datastructures.List
import List.*

/** Exercise 3.22
  *
  * Write a function that accepts two lists and constructs a new list by adding
  * corresponding elements. For example, `List(1,2,3)` and `List(4,5,6)` become
  * `List(5,7,9)`.
  */
@main def ex3_22: Unit =
  /** This implementation is not tail recursive because the result of the
    * recursive call is used to subsequently create a Cons cell.
    */
  def addPairwise(a: List[Int], b: List[Int]): List[Int] = (a, b) match
    case (Nil, _) | (_, Nil)          => Nil
    case (Cons(h1, t1), Cons(h2, t2)) => Cons(h1 + h2, addPairwise(t1, t2))
