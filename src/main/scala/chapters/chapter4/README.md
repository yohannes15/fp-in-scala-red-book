# Chapter 4

This chapter covers functional error handling. **Exceptions are side effects**. Instead we can represent failures and exceptions with ordinary values, **error as values**, and we can write higher-order functions that abstract out common patterns of error handling and recovery.

This is safer and retains referential transparency and through the use of HOFs we can preserve the primary benefit of exceptions: **consolidation of error-handling logic.**

In this chapter we will create the `Option` and `Either` types ourselves and even add some capability that isn't in the standard library.

## Topics

- Discussing the disadvantages of exceptions
- `Option` datatype
- `Either` datatype
- `Try` datatype

### Cons of exceptions

Why do exceptions break referential transparency, and why is that a problem?

```scala
def failingFn(i: Int): Int =
   val y: Int = throw Exception("fail!")
   try
     val x = 42 + 5
     x + y
   catch
     case e: Exception => 43

failingFn(12)
// Java.lang.Exception: fail! at failingFn(<console>:8) ...

// Replacing to proving y is not RT!!
def failingFn2(i: Int): Int =
  try
    val x = 42 + 5
    x + ((throw Exception("fail!")): Int)
  catch
    case e: Exception => 43
failingFn2(12)
// res1: Int = 43
```

We can prove `y` is not referentially transparent. Recall that any **Referentially Transparent expression may be substituted with the value it refers to, and this substitution should preserve program meaning.** If we substitute `throw Exception("fail!")` for `y` in `x + y`, it produces a different result because the exception will now be raised inside a try block that will catch the exception and return `43`. Look at `failingFn` and `failingFn2`.

Another way of understanding RT is knowing that the **meaning of RT expressions does not depend on context and may be reasoned about locally**, whereas the meaning of non-RT expressions is context-dependent and requires more global reasoning. 

For instance, the meaning of the RT expression `42 + 5` doesn’t depend on the larger expression it’s embedded in — it’s always and forever equal to `47`. But the meaning of the expression `throw Exception("fail")` is very context dependent; as we just demonstrated, **it takes on different meanings depending on which try block (if any) it’s nested within**.

The two main cons of expressions are:

- **Exceptions break RT and introduce context dependence**: Moves us away from the simple reasoning of the substitution model, making it possible to write confusing, exception-based code. This is the source of the folkloric advice that exceptions should be used only for error handling, not for control flow.
- **Exceptions are not type safe**: The type of `failingFn`, `Int => Int` tells us nothing about the fact that exceptions may occur, and the compiler will certainly not force callers of `failingFn` to make a decision about how to handle those exceptions. **If we forget to check for an exception in failingFn, this won’t be detected until runtime.**

## Alternatives to exceptions

```scala
def mean(xs: Seq[Double]): Double =
  if xs.isEmpty then
    throw new ArithmeticException("mean of empty list!")
  else xs.sum / xs.length
```

The mean function is an example of whats called a `partial function`: its not defined for some inputs.

Some options we have are:

1. Return some sort of bogus/default/sentinel value of type `Double`. We could simply return `xs.sum / xs.length` in all cases, resulting in `/0.0` when input is empty, which is `Double.NaN`
2. Return null instead of a value of the needed type
3. Forcing the caller to supply an argument that tells us what to do in case we don't know how to handle the input.

In above options 1/2's approach isn't ideal and is often done in languages that don't support exceptions well. We reject them for 2 reasons

- **Errors can silently propagate**: The caller can forget to check this condition and won't be alerted by compiler. Often error won't be detected until much later in the code.
- **Boilerplate at call sites**, with explicit if checks for a real result: This gets magnified if you happen to be calling several functions.
- **Not applicable to polymorphic code**: For some output types, we might not even have a sentinel value of that type even if we wanted to! Consider a function like `max`, which finds the maximum value in a sequence according to a custom comparion function like below. If the input is empty, we can't invent a value of type A, nor can null be used here, since null is only valid for nonprimitive types, and A may in fact be a primitive like Double or Int.

```scala
def max[A](xs: Seq[A])(greater: (A, A) => Boolean): A = ???
```

- **Demands special policy or calling convention of callers**: Proper use of the mean fn would require callers to do something other than call mean and make use of the result. Giving functions special policies like this makes it difficult to pass them to higher-order functions, which must treat all arguments uniformly.

In option number 3 above, it would be like below. Why is this bad?

```scala
def mean(xs: Seq[Double], onEmpty: Double): Double =
  if xs.isEmpty then onEmpty
  else xs.sum / xs.length
```

This makes mean into a total function, but it has several drawbacks:

- **Requires immediate callers to have direct knowledge of how to handle the undefined case and limits them to returning a `Double`**. What if mean is called as part of a larger computation and we'd like to abort that computation if mean is undefined. Or perhaps we'd like to take some completely different branch in the larger computation in this case. Simply passing an `onEmpty` parameter doesn't give us this freedom. We need a way to defer the decision of how to handle undefined cases so they can be dealt with at the most appropriate level.

## The Option data type

The solution is **explicitly representing that a function may not always have an answer in the return type. We can think of this as deferring to the caller for the error-handling strategy.**.

Option has two cases: `Some` (defined) and `None` (undefined)

```scala
import Option.{Some, None}

// return type now reflects possibility that result may not always be defined
// mean is also now a total function (always returns a result of the declared type)
def mean(xs: Seq[Double]): Option[Double] =
  if xs.isEmpty then None
  else Some(xs.sum / xs.length)
```

Using this type, invalid inputs now return `None` instead of sentinel values like `-99999999`. The choice of the special/sentinel value is ambiguous, and compiler can't check that the caller handles it correctly. **With `Option`, every valid output is wrapped in `Some` and compiler forces the caller to deal explicity with the possibility of failure.**

Partial functions are abound in programming, and `Option` and `Either` is typically how this partiality is dealt with in FP. Some examples where Option is used is in `Map` lookup for a given key and `headOption` and `lastOption` defined for lists and other iterables.

### Basic Functions on Option

`Option` can be thought of like a `List` that can contain at most 1 element, and many of the `List` functions we saw have analogous functions on Option.

## Misc Notes

### Throw is an expression

```scala
val y: Int = throw Exception("fail!")
```

Because throw is an expression in Scala, it evaluates to the bottom type, `Nothing`. Since Nothing is a subclass of every other type, the compiler allows it to be assigned to an Int. However, any attempt to access the variable y will crash the program with an exception

### About Checked Exceptions

While `Java’s` *checked exceptions* force a decision about whether to handle or reraise an error, they result in significant boilerplate for callers. More importantly, they don’t work for higher-order functions, which can’t possibly be aware of the specific exceptions that could be raised by their arguments. For example, consider the map function defined for List:

```scala
def map[A, B](l: List[A], f: A => B): List[B]
```

This function is clearly useful, highly generic, and at odds with the use of checked exceptions; we can’t have a version of map for every single checked exception that could possibly be thrown by f. Even if we wanted to do this, how would map know what exceptions were possible? This is why generic code, even in Java, so often resorts to using RuntimeException or some common checked Exception type. 

There is active research for this. Scala 3 has some [experimental features](https://docs.scala-lang.org/scala3/reference/experimental/canthrow.html) to try to address this.

---

### Sum on sequences

`sum` is defined as a method on `Seq` only if the elements of the sequence are numeric. The standard library accomplishes this trick with implicits.

---

### Partial vs Total function

A function is partial if its only defined for some inputs. It may also be partial if it doesn’t terminate for some inputs. We won’t discuss this form of partiality here, since it’s not a recoverable error, so there’s no question of how best to handle it. A function is typically partial because it makes some assumptions about its inputs that aren’t implied by the input types.

A function is total if its defined for all inputs. it takes each value of the input type to exactly one value of the output type.

---

### Variance Notes

The `B >: A` type parameter on the `getOrElse` and `orElse` functions indicates that `B` must be equal to or a supertype of `A`. It’s needed to convince Scala that it’s still safe to declare `Option[+A]` as covariant in `A`. [See chapter notes](https://github.com/fpinscala/fpinscala/wiki/Chapter-4:-Handling-errors-without-exceptions) for more details. It’s unfortunately somewhat complicated but a necessary complication in Scala; fortunately, fully understanding subtyping and variance isn’t essential for our purposes.

### Covariant and contravariant positions

A type is in **covariant** position (positive) if it is in the result type of a function, or more generally is the type of a value that is produced.

A type is in **contravariant** position (negative) if it's in the argument type of a function, or more generally is the type of a value that is consumed.

For example, in `def foo(a: A): B`, the **type A is in contravariant position** and **B is in covariant position**, all things being equal.

### By-name vs named arguments

The `default: => B` says that the argument is of type `B`, but it won't be evaluated until its needed by the function. This is called a **by-name parameter** and is a parameter that is only evaluated when it is actually used inside the function body. This is opposite to the usual **named arguments**
