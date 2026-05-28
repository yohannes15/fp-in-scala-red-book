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
    ???

  def double(rng: RNG): (Double, RNG) =
    ???

  def intDouble(rng: RNG): ((Int, Double), RNG) =
    ???

  def doubleInt(rng: RNG): ((Double, Int), RNG) =
    ???

  def double3(rng: RNG): ((Double, Double, Double), RNG) =
    ???
