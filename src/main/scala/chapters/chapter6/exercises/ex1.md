## Exercises 6.1

Write a function that uses `RNG.nextInt` to generate a random integer between 0 and Int.MaxValue (inclusive). Make sure to handle the corner case when nextInt returns Int.MinValue, which doesn’t have a nonnegative counterpart.

```scala
def nonNegativeInt(rng: RNG): (Int, RNG)
```

## Solution

Look at random/RNG.scala
