## Exercise 5.3

Write the function `takeWhile` for returning all starting elements of a LazyList that match the given predicate:

You can use `take` and `toList` together to inspect lazy lists in the REPL. 

For example, try printing `LazyList(1,2,3).take(2).toList`.

```scala
def takeWhile(p: A => Boolean): LazyList[A]
```

## Solution

Look at datastructures/LazyList.scala
