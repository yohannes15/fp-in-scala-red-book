## Exercise 7.1

`Par.map2` is a new higher-order function for combining the result of two parallel computations. What is its signature? Give the most general signature possible (without assuming it works only for Int).

## Solution

```scala
def map2[A, B, C](pa: Par[A], pb: Par[B], f: (A, B) => C): Par[C] = ???

extension [A](pa: Par[A]) def map2[B, C](pb: Par[B])(f: (A, B) => C): Par[C] = ???
```
