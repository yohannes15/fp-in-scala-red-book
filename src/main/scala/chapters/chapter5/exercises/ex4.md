## Exercise 5.4

Implement `forAll`, which checks that all elements in the LazyList match a given predicate. Your implementation should terminate the traversal as soon as it encounters a nonmatching value:

```scala
def forAll(p: A => Boolean): Boolean
```

## Solution

Look at datastructures/LazyList.scala
