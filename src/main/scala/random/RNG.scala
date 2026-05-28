package random

import scala.annotation.tailrec

type Rand[+A] = RNG => (A, RNG)

trait RNG:
  def nextInt: (Int, RNG)

case class SimpleRNG(seed: Long) extends RNG:
  def nextInt: (Int, RNG) =
    val newSeed = (seed * 0x5deece66dL + 0xbL) & 0xffffffffffffL
    val nextRNG = SimpleRNG(newSeed)
    val n = (newSeed >>> 16).toInt
    (n, nextRNG)

  val int: Rand[Int] = rng => rng.nextInt

  def nonNegativeInt(rng: RNG): (Int, RNG) =
    val (i, r) = rng.nextInt
    (if i < 0 then -(i + 1) else i, r)

  /*
  def double(rng: RNG): (Double, RNG) =
    val (i, r) = nonNegativeInt(rng)
    (i / Int.MaxValue.toDouble + 1, r)
   */

  val double: Rand[Double] =
    map(nonNegativeInt)(i => i / Int.MaxValue.toDouble + 1)

  def intDouble(rng: RNG): ((Int, Double), RNG) =
    val (i, r) = rng.nextInt
    val (d, r2) = double(r)
    ((i, d), r2)

  def doubleInt(rng: RNG): ((Double, Int), RNG) =
    val ((i, d), r) = intDouble(rng)
    ((d, i), r)

  def double3(rng: RNG): ((Double, Double, Double), RNG) =
    val (d, r) = double(rng)
    val (d2, r2) = double(r)
    val (d3, r3) = double(r2)
    ((d, d2, d3), r3)

  def ints(count: Int)(rng: RNG): (List[Int], RNG) =
    @tailrec
    def go(i: Int, r: RNG, acc: List[Int]): (List[Int], RNG) =
      if i <= 0 then (acc, r)
      else
        val (n, nxtR) = r.nextInt
        go(i - 1, nxtR, n :: acc)

    go(count, rng, Nil)

  def unit[A](a: A): Rand[A] =
    rng => (a, rng)

  def map[A, B](s: Rand[A])(f: A => B): Rand[B] =
    rng =>
      val (a, rng2) = s(rng)
      (f(a), rng2)

  def nonNegativeEven: Rand[Int] =
    map(nonNegativeInt)(i => i - (i % 2))

  def map2[A, B, C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
    rng0 =>
      val (a, rng1) = ra(rng0)
      val (b, rng2) = rb(rng1)
      (f(a, b), rng2)

  def both[A, B](ra: Rand[A], rb: Rand[B]): Rand[(A, B)] =
    map2(ra, rb)((_, _))

  // same functions as above, but using both
  val randIntDouble: Rand[(Int, Double)] = both(int, double)
  val randDoubleInt: Rand[(Double, Int)] = both(double, int)

  def sequence[A](rs: List[Rand[A]]): Rand[List[A]] =
    rs.foldRight(unit(Nil))((r, acc) => map2(r, acc)(_ :: _))

  def intsViaSequence(count: Int): Rand[List[Int]] =
    sequence(List.fill(count)(int))
