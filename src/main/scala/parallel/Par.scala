package parallel

import java.util.concurrent.*

opaque type Par[A] = ExecutorService => Future[A]

object Par:
  extension [A](pa: Par[A])
    /** run is where the parallelism actually gets implemented, everything else
      * in [[Par]] is a description of parallel computation.
      */
    def run(s: ExecutorService): Future[A] = pa(s)
    def map2[B, C](pb: Par[B], f: (A, B) => C): Par[C] =
      ???

  def unit[A](a: A): Par[A] = ???
  def lazyUnit[A](a: => A): Par[A] = fork(unit(a))
  def fork[A](a: => Par[A]): Par[A] = ???
