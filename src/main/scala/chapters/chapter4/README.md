# Chapter 4

This chapter covers functional error handling. **Exceptions are side effects**. Instead we can represent failures and exceptions with ordinary values, **error as values**, and we can write higher-order functions that abstract out common patterns of error handling and recovery.

This is safer and retains referential transparency and through the use of HOFs we can preserve the primary benefit of exceptions: **consolidation of error-handling logic.**

In this chapter we will create the `Option` and `Either` types ourselves and even add some capability that isn't in the standard library.

## Topics

- Discussing the disadvantages of exceptions
- `Option` datatype
- `Either` datatype
- `Try` datatype

## Cons of exceptions

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

### Alternatives to exceptions

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

Option doesn’t tell us anything about what went wrong in the case of an exceptional condition. All it can do is give us None, indicating there’s no value to be had.

### Basic Functions on Option

`Option` can be thought of like a `List` that can contain at most 1 element, and many of the `List` functions we saw have analogous functions on Option.

The `map` function can be used to transform the result inside an Option, if it exists. We can think of it as proceeding with a computation on the assumption that an error hasn't occurred; its also a way of deferring the error handling to later code

```scala
case class Employee(
  name: String,
  department: String,
  manager: Option[Employee])
def lookupByName(name: String): Option[Employee] = ???
val joeDepartment: Option[String] = lookupByName("Joe").map(_.department)

lookupByName("Joe").map(_.department)
// Joe's department if Joe is an employee. None if joe is not an employee

lookupByName("Joe").flatMap(_.manager)
// Some(Manager) if joe has a manager. None if joe is not an employee or doesn't have a manager

lookupByName("Joe").map(_.department).getOrElse("Default Dept.")
// Joe's department if joe is an employee. "Default dept" if not
```

With `flatMap` and `f: A => Option[B]` we can construct a computation with multiple stages, any of which may fail, and the computation will abort as soon as the first failure is encountered, since None.flatMap(f) will immediately return None, without running f.

A common pattern is transforming an `Option` via calls to `map`, `flatMap` and/or `filter` and then using `getOrElse` to do error handling at the end.

```scala
val dept: String = 
  lookupByName("Joe").map(_.department).filter(_ != "Accounting").getOrElse("Default Apt")
```

`getOrElse` is used here to convert from an `Option[String]` to a `String` by providing a default department in case the key "Joe" didn’t exist in the Map or Joe’s department was "Accounting". `orEls`e is similar to `getOrElse`, except that we return another Option if the first is undefined. This is often useful when we need to chain together possibly failing computations, trying the second if the first hasn’t succeeded.

**NOTE** that we don't have to check for None at each stage of the computation; we can apply several transformations and then check for and handle `None` when we are ready. We also get additional safety, since **`Option[A]` is a different type than `A` and the compiler won't let us forget to defer or handle the possibility of `None`.**

A common idiom is using `o.getOrElse(throw Exception(...))` to convert the `None` case back to an Exception. The general rule is using exceptions only:

- **If no reasonable program would ever catch the exception**: If for some callers the exception might be a recoverable error, we use `Option / Either` to give them flexibility. When in doubt, avoid use of exceptions especially as a beginner as error values are usually better than exceptions.

## Option Composition / Lifting and Wrapping Exception-Oriented APIs

It may be easy to jump to the conclusion that once we start using `Option`, it spreads throughout our entire code base. One can imagine how any callers of methods that take or return `Option` will have to be modified to handle either Some or None; but actually this doesn’t happen **because we can *lift* ordinary functions to become functions that operate on `Option`.**

For example, the `map` function lets us operate on values of the `Option[A]` type using a function of the `A => B` type, which returns `Option[B]`. Another way of looking at this is that map turns a function `f` of type `A => B` into a function of type `Option[A] => Option[B]`.

```scala
/** returns a function which maps None to None and applies f to the contents of Some.
  * f need not be aware of the Option type at all.
  */
def lift[A, B](f: A => B): Option[A] => Option[B] = 
  _.map(f)
```

This tells us that any function we already have can be transformed (via `lift`) to operate within the context of an `Option` value. 

```scala
val absOpt: Option[Double] => Option[Double] = lift(math.abs)
val ex1 = absOpt(Some(-1.0))
// ex1: Option[Double] = Some(1.0)
```

```scala
/** Secret formula for computing an annual car insurance premium from any 2 key factors */
def insuranceRateQuote(age: Int, numberOfSpeedingTickets: Int): Double = ???
```

We want to be able to call this function, but if the user is submitting their age and number of speeding tickets in a web form, these fields will arrive as simple strings that we have to (try to) parse into integers. This parsing may fail; given a string, `s`, we can attempt to parse it into an `Int` using `s.toInt`, which throws a `NumberFormatException` if the string isn’t a valid integer.

We can write a utility function that converts that exception into a `None` like below but we will run into a problem? our Quote function takes two `Int` values not `Option[Int]`. Do we have to rewrite `insuranceRateQuote` to take `Option[Int]` values instead? **NO**, and changing `insuranceRateQuote` would be entangling concerns, forcing it to be aware that a prior computation may have failed, not to mention that we may not have the ability to modify `insuranceRateQuote`—perhaps it’s defined in a separate module we don’t have access to. 

Instead we lift `insuranceRateQuote` to operate in the context of two Optional values. We could do this by using explicit pattern matching in the body of `parseInsuranceRateQuote` but thats going to be tedious? Instead we will use `map2` to combine two optional values.

```scala
def toIntOption(s: String): Option[Int] = 
  try Some(s.toInt)
  catch case _: NumberFormatException => None

def parseInsuranceRateQuote(age: String, numberOfSpeedingTickets: String): Option[Double] =
  val optAge: Option[Int] = toIntOption(age)
  val optTickets: Option[Int] = toIntOption(numberOfSpeedingTickets)
  // if either parse fails, this will immediately return None
  map2(optAge, optTickets)(insuranceRateQuote)
```

The `map2` function means we never need to modify any existing functions of two arguments to make them `Option`-aware. We can lift them to operate in the context of `Option` after the fact. We can already see how we might define `map3`, `map4`, and `map5` ...

### Sequence and Traverse 

Sometimes we’ll want to map over a list using a function that might fail, returning `None` if applying it to any element of the list returns None. For example, what if we have a whole list of `String` values that we wish to parse to `Option[Int]`? In that case, we can simply sequence the results of the map:

```scala
def sequence[A](as: List[Option[A]]): Option[List[A]] = 
  as.foldRight(Some(Nil)) {
    case (a, acc) => map2(a, acc)(_ :: _)
  }

def parseInts(as: List[String]): Option[List[Int]] =
  sequence(as.map(a => toIntOption(s)))
```

Unfortunately, this is inefficient since it traverses the list twice—first to convert each `String` to an `Option[Int]` and second to combine these `Option[Int]` values into an `Option[List[Int]]`. **Wanting to sequence the results of a map this way is a common enough occurrence** to warrant a new generic function, `traverse`, with the following signature:

```scala
def traverse[A, B](as: List[A])(f: A => Option[B]): Option[List[B]] = 
  as.foldRight(Some(Nil)) {
    case (a, acc) => map2(f(a), acc)(_ :: _)  
  }
```

In this defintion, sequence can be implemented easily using `traverse`. We pass the identity function to traverse since each element in our input list is already an option.

```scala
traverse(as)(identity) || traverse(as)(a => a)
```

### For Comprehensions

Since lifting functions is so common in Scala, Scala provides a syntactic construct called the `for-comprehension`, which it expands automatically to a series of `flatMap` and `map` calls. Let’s look at how map2 could be implemented with for-comprehensions. Here’s our original version

```scala
def map2[A, B, C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C] =
  a.flatMap(aa => b.map(bb => f(aa, bb)))
```

And here’s the exact same code written as a for-comprehension:

```scala
def map2[A, B, C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C] =
  for
    aa <- a
    bb <- b
  yield f(aa, bb)
```

The `yield` may make use of any of the values on the left side of any previous <- binding. The compiler desugars the bindings to `flatMap` calls, with the final binding and yield being converted to a call to map.

You should feel free to use for-comprehensions in place of explicit calls to `flatMap` and `map`. Likewise, feel free to rewrite a for-comprehension as a sequence of flat-Map calls followed by a final map if doing so helps you understand the expression. For-comprehensions are purely a syntax convenience.

### Adapting functions to options

Between `map, lift, sequence, traverse, map2, map3`, and so on, **you should never have to modify any existing functions to work with optional values.**

## The Either data type

```scala
enum Either[+E, +A]:
  case Left(value: E)
  case Right(value: A)
```

`Either` has only two cases, just like `Option`. The essential difference is that both cases carry a value. The Either data type represents, in a very general way, values that can be one of two things. We can say that it’s a *disjoint union* of two types. When we use it to indicate success or failure, by convention. the `Right` constructor is reserved for the *success* case (a pun on right, meaning correct), and `Left` is used for failure. We’ve given the left type parameter the suggestive name E (for error).

`Either` is also often used more generally to encode one of two possibilities in cases where it isn’t worth defining a fresh data type. We’ll see some examples of this throughout the book. 

```scala 
import Either.{Left, Right} 
import scala.util.control.NonFatal

def mean(xs: Seq[Double]): Either[String, Double] =
  if xs.isEmpty then
    Left("mean of empty list!")
  else
    Right(xs.sum / xs.length)

def safeDiv(x: Int, y: Int): Either[Throwable, Int] =
  try Right(x / y)
  // The NonFatal pattern match ensures we do not catch fatal errors, e.g. OutOfMemoryException.
  catch case NonFatal(t) => Left(t)
```

We can extract a more general function, `catchNonFata`l, which factors out this common pattern of converting thrown exceptions to values:

```scala
def catchNonFatal[A](a: => A): Either[Throwable, A] =
  try Right(a)
  catch case NonFatal(t) => Left(t)
```

This function is general enough to be defined on the `Either` companion object since it’s not tied to a single use case.

After we have flatMaps and maps, Either can be used in for-comprehensions. Also, now we get information about the actual exception that occurred, rather than just getting back None in the event of a failure.

```scala
def parseInsuranceRateQuote(
    age: String,
    numberOfSpeedingTickets: String): Either[Throwable,Double] =
  for
    a <- Either.catchNonFatal(age.toInt)
    tickets <- Either.catchNonFatal(numberOfSpeedingTickes.toInt)
  yield insuranceRateQuote(a, tickets)

```

**Using Either to validate data**

```scala
// case classes with private constructors result in construction only being allowed in their companion objects. 
// The apply method in each companion validates the input before constructing a value.
case class Name private (value: String)
object Name:
  def apply(name: String): Either[String, Name] =
    if name == "" || name == null then Left("Name is empty.")
    else Right(new Name(name))
 
case class Age private (value: Int)
object Age:
  def apply(age: Int): Either[String, Age] =
    if age < 0 then Left("Age is out of range.")
    else Right(new Age(age))
 
case class Person(name: Name, age: Age)
object Person:
  /** an application of map2 where the function Person.make validates both
    * the given name and the given age before constructing a valid Person. */
  def make(name: String, age: Int): Either[String, Person] =
    Name(name).map2(Age(age))(Person(_, _))
```

### Accumulating errors

`map2` is only able to report one error, even if both arguments are invalid (both are `Left`). To report multiple errors we need to make a few leaps. Lets start with `map2Both`

```scala 
def map2Both[E, A, B, C](
  a: Either[E, A],
  b: Either[E, B],
  f: (A, B) => C
): Either[List[E], C] = 
  (a, b) match
    case (Right(aa), Right(bb)) => Right(f(aa, bb))
    case (Left(e), Right(_)) => Left(List(e))
    case (Right(_), Left(e)) => Left(List(e))
    case (Left(e1), Left(e2)) => Left(List(e1, e2))

object Person:
  def makeBoth(name: String, age: Int): Either[List[String], Person] = 
    map2Both(Name(name), Age(age), Person(_, _))

val p = Person.makeBoth("", -1)
// Either[List[String], Person] = Left(List(Name is empty., Age is out of range.))

```

Unfortunately, `map2Both` is very limited. Consider what happens when we want to combine the result of 2 calls to `Person.makeBoth`.

```scala
val p1 = Person.makeBoth("Curry", 34)
// Either[List[String], Person] = Right(Person(Name(Curry),Age(34)))
val p2 = Person.makeBoth("Howard", 44)
// Either[List[String], Person] = Right(Person(Name(Howard),Age(44)))
val pair = map2Both(p1, p2, (_, _))
// Either[List[List[String]], (Person, Person)] = 
//    Right((Person(Name(Curry),Age(34)),Person(Name(Howard),Age(44))))
```

This compiles fine, but take a close look at the inferred type of `pair`—the left side of the `Either` now has nested lists! Each successive use of `map2Both` adds another layer of `List` to the error type. We can fix this by changing `map2Both` slightly. We’ll require the input values to already have a `List[E]` on the left side. Let’s call this new variant `map2All`.

```scala
def map2All[E, A, B, C](
  a: Either[List[E], A],
  b: Either[List[E], B],
  f: (A, B) => C
): Either[List[E], C] =
  (a, b) match
    case (Right(aa), Right(bb)) => Right(f(aa, bb))
    case (Left(es), Right(_)) => Left(es)
    case (Right(_), Left(es)) => Left(es)
    case (Left(es1), Left(es2)) => Left(es1 ++ es2)

val pair = map2All(p1, p2, (_, _))
// Either[List[String], (Person, Person)] =
//    Right((Person(Name(Curry),Age(81)),Person(Name(Howard),Age(96))))

```

Now let's try implementing a variant of `traverse` that returns all errors. Changing just the return type gives us signature like the following:

```scala
def traverseAll[E, A, B](as: List[A], f: A => Either[List[E], B]): Either[List[E], List[B]] =
  as.foldRight(Right(Nil): Either[List[E], List[B]])((a, acc) =>
    map2All(f(a), acc, _ :: _)
  )

def sequenceAll[E, A](as: List[Either[List[E], A]]): Either[List[E], List[A]] =
  traverseAll(as, identity)
```

## Validated data type

`Either` is a Monad, operations are chained using `bind` or `flatMap`. This means next step in validation depends on result of previous step. If step1 fails, step2 is never run. Fails Fast.


`Either[List[E], A]`, along with functions like `map2All`, `traverseAll`, and `sequenceAll`, gives us the ability to accumulate errors. Instead of defining these related functions in this ad hoc way, we can name this accumulation behavior, as `Validated`. A value of `Validated[E, A]` can be converted to an `Either[List[E], A]` and vice versa. We will define the above `...All` functions here and we no longer need the `...All` suffix, since it inherently supports the accumulation of errors

`Validated` is an Applicative Functor but **not a monad**. Because it is not a monad, operations can't be chained sequentially based on previous results. Instead, it evaluates all validations independently and groups all failures together.

```scala
enum Validated[+E, +A]:
  case Valid(get: A)
  case Invalid(errors: List[E])
```

Our type accumulates a `List[E]` of errors. Why `List` though? What if we want to use some other type, like `Vector` or `Tree` or ... As of right now only `map2` depends on errors being modeled as a list (concat errors from two `Invalid` values). Lets redefine as below

```scala
enum Validated[+E, +A]:
  case Valid(get: A)
  case Invalid(errors: E)
```

At first glance, it appears we’ve taken a step backward by defining `Validated` very much like `Either` is defined. The key difference between this version of `Validated` and `Either` is in the signature of `map2`. In particular, we need a way to combine two invalid values into a single invalid value. In the previous definition of Validated, where Invalid wrapped a `List[E]`, our combining action was *list concatenation*. But with this new definition, **we need a way to combine two E values into a single E value**, and we know nothing about E. It seems like we’re stuck, but we can modify the signature of `map2` and simply ask for such a combining action.

```scala
enum Validated[+E, +A]:
  case Valid(get: A)
  case Invalid(errors: E)

def map2[EE >: E, B, C](
  b: Validated[EE, B])(
  f: (A, B) => C)(
  combineErrors: (EE, EE) => EE
): Validated[EE, C] = // Look at datastrucutres/Validated.scala
```

**Final Note**: `Validted`, `sequence`, `traverse`, and `map2` not provided by standard library. Provided by `Cats`.

## Misc Notes

### `Try[A]`

The `Try` type is like `Either`, except errors are represented as `Throwable` values instead of arbitrary types. By constraining errors to be subtypes of `Throwable`, the Try type is able to provide various convenience operations for code that throws exceptions.

This is basically equivalent to `Either[Throwable, A]`. It has operations that are specialized for working with exceptions. The `Try` type represents a computation that may fail during evaluation by raising an exception. 

[Read API here](https://nightly.scala-lang.org/api/scala/util/Try.html). 

The apply method on the `Try` companion object is equivalent to the catchNonFatal method we defined earlier. `Either` lets us track a precise error type (e.g., `Either[NumberFormatException, Int]`), whereas `Try` tracks `Throwable`.

```scala
enum Try[+T]:
  case Failure(exception: Throwable)
  case Success(value: T)

// LOOK AT datastructures/Try.scala for more
```


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

The `default: => B` says that the argument is of type `B`, but it won't be evaluated until its needed by the function. This is called a **by-name parameter** and is a parameter that is only evaluated when it is actually used inside the function body. This is opposite to the usual **named arguments**. Examples from this chapter are `orElse`, `getOrElse` and `catchNonFatal`.

In Scala, **a non-strict (or lazy)** function is one that may choose not to evaluate one or more of its arguments. By default, Scala evaluates arguments strictly, but you can define non-strict parameters using by-name parameters (e.g., => A). This defers evaluation until the argument is accessed within the function body.

### Parameter lists

```scala
def map2[A, B, C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C]
```

Note that we have two parameter lists here; the first parameter list takes an `Option[A]` and an `Option[B]`, and the second parameter list takes a function `(A, B) => C`. To call this function, we supply values for each parameter list—for example, `map2(oa, ob) (_ + _)`. We could have defined this with a single parameter list instead, though it’s common style to use **two parameter lists when a function takes multiple parameters and the last parameter is itself a function**.

Doing so allows a syntax variation when passing multiline anonymous functions, where the final parameter list is replaced with either an *indented block* or a *brace delimited* block:

```scala
// indented block following a colon and parameter list
map2(oa, ob): (a, b) => 
  a + b
// Brace delimited block
map2(oa, ob) { (a, b) =>
  a + b
}
```

*There was another benefit of multiple parameter lists in `Scala 2`: better type inference. Scala 2 inferred type parameters on each parameter list in **succession**. If Scala 2 was able to infer a concrete type in the first parameter list, then any appearance of that type in subsequent parameter lists would be fixed (i.e., not further inferred or generalized). For example, map(List(1, 2, 3), _ + 1) from chapter 3 would fail to compile with a type inference error, but had we defined map with two parameter lists, resulting in usage like map(List(1, 2, 3))(_ + 1), compilation would have succeeded. **Scala 3 can infer type parameters from all parameter lists simultaneously**, so there are no longer type inference advantages to using multiple parameter lists.*
