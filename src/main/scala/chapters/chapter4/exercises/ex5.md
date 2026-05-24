## Exercise 4.5

Implement `traverse` function since `map` & `sequence` is a 2 pass solution and inefficient. This is common enough to require a solution. It’s straightforward to do using map and sequence, but try for a more efficient implementation that only looks at the list once. In fact, after implement `sequence` in terms of traverse.

```scala
def traverse[A, B](as: List[A])(f: A => Option[B]): Option[List[B]]
```

## Solution

Look at datastructures/Option.scala
