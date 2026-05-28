## Exercises 6.2

Write a function to generate a `Double` between 0 and 1, not including 1. Note that you can use Int.MaxValue to obtain the maximum positive integer value, and you can use x.toDouble to convert an x: Int to a Double.

```scala
def double(rng: RNG): (Double, RNG)
```

## Solution

Look at random/RNG.scala
