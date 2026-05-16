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


