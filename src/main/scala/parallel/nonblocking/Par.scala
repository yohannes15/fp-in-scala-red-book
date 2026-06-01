package parallel.nonblocking

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference

opaque type Future[+A] = (A => Unit) => Unit
opaque type Par[+A] = ExecutorService => Future[A]

object Par:
  extension [A](pa: Par[A])
    def run(es: ExecutorService): A =
      val ref = new AtomicReference[A]
      val latch = new CountDownLatch(1)
      pa(es) { a => ref.set(a); latch.countDown }
      latch.await
      ref.get

  /** simply passes value to callback. Executor service isn't needed */
  def unit[A](a: A): Par[A] =
    es => cb => cb(a)

  def fork[A](a: => Par[A]): Par[A] =
    es => cb => eval(es)(a(es)(cb))

  /** evaluate an action asynchronously using some ExecutorService */
  def eval(es: ExecutorService)(r: => Unit): Unit =
    es.submit(
      new Callable[Unit]:
        def call = r
    )
