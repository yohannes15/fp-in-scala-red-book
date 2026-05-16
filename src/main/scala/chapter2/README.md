# Chapter 2

This chapter includes mostly introductory pieces towards Functional Programming and Scala

## Topics

- Introducing Scala language
- Objects and namespaces
- Higher-order-functions intro
- **Tail Calls **
- Polymorphic functions: Abstracting over types
- Following types to implementations

## Chapter 2 Notes

### Tail Calls In Scala

```scala

def factorial(n: Int): Int =
  def go(n: Int, acc: Int): Int =    ①
    if n <= 0 then acc
    else go(n - 1, n * acc)
 
  go(n, 1)

```

A call is said to be in `tail position` if the caller does nothing other than return the value of the recursive call. For example. the recursive call to `go(n-1, n*acc)` is in tail position since the method returns the value of this recursive call directly and does nothing else with it. On the other hand, if we said `1 + go(n-1, n*acc)`, `go` would no longer be in tail position since the method would have more work to do when `go` returned its results (adding 1)

We can manually trace the execution of a recursive function to get a better understanding how evaluation proceeds. An execution trace of factorial(5) might look like the following:

```scala
factorial(5)
  go(5, 1)
    go(4, 5)
      go(3, 20)
        go(2, 60)
          go(1, 120)
            go(0, 120)
              120
```

In this trace, each recursive call increases the indent level of the trace. We may choose to render tail recursive calls without increasing the indent level. Since the recursive call in go is in tail position, we could write the trace as:

```scala
factorial(5)
  go(5, 1)
  go(4, 5)
  go(3, 20)
  go(2, 60)
  go(1, 120)
  go(0, 120)
  120
```

If recursive calls made by a function are in tail position, Scala automatically compiles the recursion to iterative loops that don’t consume call stack frames for each iteration. By default, Scala doesn’t tell us if tail call elimination was successful, but if we’re expecting this to occur for a recursive function we write, we can tell Scala compiler about this assumption using the tailrec annotation so it can give us a compile error if it’s unable to eliminate the tail calls of the function. Here’s the syntax for this:


```scala
def factorial(n: Int): Int =
  @annotation.tailrec
  def ifFunctionNotTailRecCompilerErrors(n: Int, acc: Int): Int =
    ???

  go(n, 1)
```

### ...
