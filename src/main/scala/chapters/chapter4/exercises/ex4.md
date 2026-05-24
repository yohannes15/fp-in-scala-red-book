## Exercise 4.4

Write a function `sequence` that combines a list of `Option`s into one `Option` containing a list of all the `Some` values in the original list. **If the original list contains `None` even once, the result of the function should be None**; otherwise, the result should be Some, with a list of all the values. Here is its signature:

```scala
def sequence[A](as: List[Option[A]]): Option[List[A]]
```

## Solution

Look at datastructures/Option.scala
