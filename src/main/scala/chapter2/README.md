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

### Polymorphic Functions

Also known as Generic functions. These are functions that use >= 1 type parameters in their signature, allowing them to operate on many types. Monomorphic functions have no type parameters and have static type defintion.

If a function is polymorphic in some type `A`, the only operations that can be performed on that `A` are those passed into the function as arguments. In some cases, you’ll find that the universe of possibilities for a given polymorphic type is constrained such that only one implementation is possible!

Often, and especially when writing higher-order functions, we want to write code that works for any type it’s given. These are called polymorphic functions. The type parameter list introduces type variables that can be referenced in the rest of the type signature (exactly analogous to how variables introduced in the parameter list to a function can be referenced in the body of the function)

```scala
/** Example of a function signature that can only be implemented 
* in one way. A higher-order function that takes a function of two
arguments and partially applies it. That is, if we have an A and a
fn that needs both A and B to produce C, we can get a function that
just needs B to produce C (since we already have the A). 

In simple terms -> "If I can give you a (C)arrot for an (A)pple and
a (B)anana, and you already gave me an (A)pple, you just have to give
me a (B)anana, and I'll give you a (C)arrot"
*/
def partial1[A, B, C](a: A, f: (A, B) => C): B => C =
  b => f(a, b)
```

### Currying

This is named after the mathematician Haskell Curry, who discovered the principle. It was independently discovered earlier by Moses Schoenfinkel. Currying converts a function `f` of two arguments into another function `g` of one argument that partially applies `f`.

```scala
// look at exercise 2.3/2.4 to see implementation.
// Since => associates to the right, both of these signatures are identical
def curry[A, B, C](f: (A, B) => C): A => (B => C) = ???
def curry[A, B, C](f: (A, B) => C): A => B => C = ???

def uncurry[A, B, C](f: A => B => C): (A, B) => C = ???
```

### Composition

Function composition feeds the output of one function to the input of another function.

```scala
def compose[A, B, C](f: B => C, g: A => B): A => C = ???
```

Composition is essential thus Scala's standard library provides `compose` as a method on `Function1` (interface for functions that take one argument). To compose two functions, `f` and `g`, we say `f compose g`. It also provides an `andThen` method. `f andThen g`.

```scala
val f = (x: Double) => math.Pi / 2 - x
// f: Double => Double
 
val cos = f andThen math.sin
// cos: Double => Double
```

HOF functions like `compose` don't care whether they're operating on huge functions backed by millions of lines of code or functions that are simple one-liners.

### Conclusion and Summary

Polymorphic, HOF functions often end up being extremely widely applicable, precisely because they say nothing about any particular domain and are simply abstracting over a common pattern that occurs in many contexts. 

Implementations of polymorphic functions are often significantly constrained such that we can often simply follow the types to the correct implementation. This is called **Following types to implementations** or **type-driven development**
