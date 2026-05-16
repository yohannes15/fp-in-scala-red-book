package chapter2.exercises

/** Write a recursive function to get the nth Fibonacci number. The first two
  * Fibonacci numbers are 0 and 1. The nth number is always the sum of the
  * previous two—the sequence begins 0, 1, 1, 2, 3, 5. Your definition should
  * use a local, tail-recursive function:
  */

@main def fib(n: Int): Int =
  // non tail recursive
  def loop(n: Int): Int =
    if n <= 1 then n
    else loop(n - 1) + loop(n - 2)

  /** Tail recursive function. Instead of waiting for the recursive calls to
    * "return" and then adding them (e.g., fib(n-1) + fib(n-2)), this method
    * calculates the next value immediately and passes it forward into the next
    * call.
    *
    * @param n
    *   remainingSteps
    * @param current
    *   The Fibonacci number at the current step in the sequence.
    * @param next
    *   The Fibonacci number immediately following current
    * @return
    */
  def go(n: Int, current: Int, next: Int): Int =
    if (n <= 0) current
    else go(n - 1, next, current + next)

  val fibNum = go(n, 0, 1)
  println(s"Fibonacci number: ${fibNum}")
  fibNum
