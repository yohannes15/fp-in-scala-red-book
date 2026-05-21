## Exercise 3.8

See what happens when you pass Nil and Cons themselves to foldRight, like this: 

```scala
foldRight(List(1, 2, 3), Nil: List[Int], Cons(_, _))
```


What do you think this says about the relationship between foldRight and the data constructors of List?

## Solution

It evaluates to `Cons(1, Cons(2, Cons(3, Nil)))`. Recall that foldRight(as, acc, f) replaces Nil with acc and Cons with f. When we set acc to Nil and f to Cons, our replacements are all identities.
