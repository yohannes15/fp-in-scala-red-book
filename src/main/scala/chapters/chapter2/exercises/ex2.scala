package chapters.chapter2.exercises

import scala.annotation.tailrec

/** Implement isSorted, which checks whether an Array[A] is sorted according to
  * a given comparison function, gt, which returns true if the first parameter
  * is greater than the second parameter:
  */
@main def isSortedPolymorphism: Unit =
  println(isSorted(Array(1, 2, 3), _ > _))
  println(isSorted(Array(1, 2, 1), _ > _))
  println(isSorted(Array(3, 2, 1), _ < _))
  println(isSorted(Array(1, 2, 3), _ < _))
  // SAME OUTPUTS ON BETTER SOLN
  println("\nBetter Solution Results \n")
  println(BetterSolution.isSorted(Array(1, 2, 3), _ > _))
  println(BetterSolution.isSorted(Array(1, 2, 1), _ > _))
  println(BetterSolution.isSorted(Array(3, 2, 1), _ < _))
  println(BetterSolution.isSorted(Array(1, 2, 3), _ < _))

def isSorted[A](as: Array[A], f: (A, A) => Boolean): Boolean =

  /** NOTE: I am not sure why I decided to start with prev instead of just
    * working with next as n + 1. Foolish choice
    */
  @tailrec def go(n: Int, prev: A): Boolean =
    if n >= as.length then
      true
    else if f(prev, as(n)) then
      false
    else go(n + 1, as(n))

  if as.length <= 1 then
    true
  else go(2, as(1))

object BetterSolution:
  def isSorted[A](as: Array[A], gt: (A, A) => Boolean): Boolean =

    @tailrec def loop(n: Int): Boolean =
      if n + 1 >= as.length then true
      else if gt(as(n), as(n + 1)) then false
      else loop(n + 1)

    loop(0)
