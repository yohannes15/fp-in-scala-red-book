package random

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
    ???

  def doubleInt(rng: RNG): ((Double, Int), RNG) =
    ???

  def double3(rng: RNG): ((Double, Double, Double), RNG) =
    ???

  def ints(count: Int)(rng: RNG): (List[Int], RNG) =
    ???
