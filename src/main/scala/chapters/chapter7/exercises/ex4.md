## Exercise 7.4

This API already enables a rich set of operations. Here’s a simple example. Using `lazyUnit`, write a function to convert any function A => B to one that evaluates its result asynchronously:

```scala
def asyncF[A, B](f: A => B): A => Par[B]
```

## Solution

Look at `parallel/Par.scala`
