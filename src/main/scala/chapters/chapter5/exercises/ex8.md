## Exercise 5.8

Generalize `ones` slightly to the function `continually`, which returns an infinite LazyList of a given value:

```scala
lazy val ones: LazyList[Int] = LazyList.cons(1, ones)
```

```scala
def continually[A](a: A): LazyList[A]
```

## Solution

Look at datastructures/LazyList.scala

