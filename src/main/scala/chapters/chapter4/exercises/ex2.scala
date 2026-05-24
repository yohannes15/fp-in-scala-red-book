package chapters.chapter4.exercises

import datastructures.Option
import Option.*

/** Implement the variance function in terms of [[flatMap]]. If the mean of a
  * sequence is m, the variance is the mean of math.pow(x - m, 2) for each
  * element x in the sequence.
  *
  * Read the definition of variance aka spread online. In short:
  *
  * The mean is simply the average of a dataset (the center point). The variance
  * measures how far the numbers in that dataset are spread out from the mean.
  */
@main def ex2: Unit =
  ???

def variance(xs: Seq[Double]): Option[Double] =
  mean(xs).flatMap(m => mean(xs.map(x => math.pow(x - m, 2))))

def mean(xs: Seq[Double]): Option[Double] =
  if xs.isEmpty then None
  else Some(xs.sum / xs.length)
