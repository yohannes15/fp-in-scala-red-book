import datastructures.List
import List.*

/** ## Exercise 3.17
  *
  * Write a function that turns each value in a `List[Double]` into a String.
  * You can use the expression `d.toString` to convert some `d: Double` to a
  * `String`.
  */
@main def ex3_17: Unit =
  def doubleToString(l: List[Double]): List[String] =
    foldRight(l, Nil, (d, acc) => Cons(d.toString, acc))
