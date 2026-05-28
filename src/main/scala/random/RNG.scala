package random

import scala.annotation.tailrec

trait RNG:
  def nextInt: (Int, RNG)

case class SimpleRNG(seed: Long) extends RNG:
  def nextInt: (Int, RNG) =
    val newSeed = (seed * 0x5deece66dL + 0xbL) & 0xffffffffffffL
    val nextRNG = SimpleRNG(newSeed)
    val n = (newSeed >>> 16).toInt
    (n, nextRNG)

  def nonNegativeInt(rng: RNG): (Int, RNG) =
    val (i, r) = rng.nextInt
    (if i < 0 then -(i + 1) else i, r)

  def double(rng: RNG): (Double, RNG) =
    val (i, r) = nonNegativeInt(rng)
    (i.toDouble / Int.MaxValue.toDouble + 1, r)

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
