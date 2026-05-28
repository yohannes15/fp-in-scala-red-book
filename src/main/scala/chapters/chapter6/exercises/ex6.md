## Exercise 6.6

Unfortunately, map isn’t powerful enough to implement intDouble and doubleInt from exercise 6.3. Instead, we need a new function, `map2`, that can combine two RNG actions into one using a binary rather than unary function.

Write the implementation of `map2` based on the following signature. This function takes two actions, `ra` and `rb`, and a function, `f`, for combining their results and returns a new action that combines them:

```scala
def map2[A, B, C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C]
```

We only have to write the map2 function once, and then we can use it to combine arbitrary RNG state actions. For example, if we have an action that generates values of type A and another to generate values of type B, then we can combine them into one action that generates pairs of both A and B

```scala
def both[A, B](ra: Rand[A], rb: Rand[B]): Rand[(A, B)] =
  map2(ra, rb)((_, _))
```

We can use this to reimplement intDouble and doubleInt from exercise 6.3 more succinctly

## Solution

Look at random/RNG.scala
