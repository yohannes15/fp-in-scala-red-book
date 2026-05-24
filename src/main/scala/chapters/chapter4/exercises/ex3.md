## Exercise 4.3

Write a generic function `map2` that combines two `Option` values using a binary function. **If either Option value is None, then the return value is too**. Here is its signature:

```scala
def map2[A, B, C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C]
```

## Solution

Look at datastructures/Option.scala
