package parallel

import java.util.concurrent.*

opaque type Par[A] = ExecutorService => Future[A]

object Par:
  extension [A](pa: Par[A])
    def map2[B, C](pb: Par[B], f: (A, B) => C): Par[C] =
      ???

  def unit[A](a: => A): Par[A] = ???
  def get[A](a: Par[A]): A = ???
