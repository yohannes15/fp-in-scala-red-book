## Exercise 5.1

Write a function to convert a `LazyList` to a `List`, which will force its evaluation and let you look at it in the REPL. You can convert to the regular List type in the standard library, and you can place this and other functions that operate on a LazyList inside the LazyList enum:

```scala
def toList: List[A]
```

## Solution

Look at datastructures/LazyList.scala
