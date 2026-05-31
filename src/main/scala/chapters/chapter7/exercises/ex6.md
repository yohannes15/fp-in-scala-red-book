## Exercise 7.6

Implement parFilter, which filters elements of a list in parallel:

```scala
def parFilter[A](as: List[A])(f: A => Boolean): Par[List[A]]
```

## Solution

Look at parallel/Par.scala
