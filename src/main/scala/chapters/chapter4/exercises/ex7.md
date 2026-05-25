## Exercise 4.7

Implement sequence and traverse for Either. These should return the first error that’s encountered if there is one:

```scala
def sequence[E, A](as: List[Either[E, A]]): Either[E, List[A]] = ???
def traverse[E, A, B](as: List[A])(f: A => Either[E, B]): Either[E, List[B]] = ???
```

## Solution

Look at datastructures/Either.scala
