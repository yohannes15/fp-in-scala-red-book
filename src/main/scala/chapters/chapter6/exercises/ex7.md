## Exercise 6.7

**Hard**: If you can combine two RNG actions, you should be able to combine an entire list of them. Implement sequence for combining a List of actions into a single action. Use it to reimplement the ints function you wrote before. 

For the latter, you can use the standard library function `List.fill(n)(x)` to make a list with x repeated n times:

```scala
def sequence[A](rs: List[Rand[A]]): Rand[List[A]]
```

## Solution

Look at random/RNG.scala
