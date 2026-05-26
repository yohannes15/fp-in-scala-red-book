# Chapter 5

This chapter covers strictness and laziness.

## Topics

- Strictness vs nonstrictness
- Introducing lazy lists
- Separating program description from evaluation

## Intro

In chapter 3, we talked about purely functional data structures, using singly linked lists as an example. We noted that each of these operations makes its own pass over the input and constructs a fresh list for the output.

```scala
/* 
How a program is evaluated using substitution model. In each
transformation scala produces a temporary list that gets used as
input to the next transformation and is immediately discarded
*/
List(1,2,3,4).map(_ + 10).filter(_ % 2 == 0).map(_ * 3)
List(11,12,13,14).filter(_ % 2 == 0).map(_ * 3)
List(12,14).map(_ * 3)
List(36,42)
```

**Question**: Wouldn’t it be nice if we could fuse sequences of transformations like this into a single pass and avoid creating temporary data structures? We could use a `while` loop, but ideally, we’d like to have this done automatically while retaining high-level compositional style. We want to compose our programs using higher-order functions, like `map` and `filter`, instead of writing monolithic loops.

**Answer**: We can accomplish this fusion by using **nonstrictness (or, less formally, laziness).**. We will define meaning of lazy and work through an implementation of `LazyList` that fuses sequences of transformations. **Nonstrictness is a fundamental technique for improving on the efficiency and modularity of functional programs in general not just this scenario.**

## Strict vs nonstrict functions

**Nonstrictness** is a property of a function. It means that the function may choose not to evaluate one or more of its arguments. In contrast, a **strict** function always evaluates its arguments. Strict functions are the norm usually and most programming languages only support functions that except their arguments fully evaluated. By default, any function defintion in Scala is *strict*, we need to instruct it otherwise.

**Nonstrict functions** examples: 

- Boolean functions `&&` and `||`: We may think of these as built in syntax, but you can think of them as nonstrict functions. `&&` takes two 2 boolean arguments but only evaluates the second argument if the first is true. `||` only evaluates its second argument if the first is false.

- `if` control construct: Again even if it is a language construct, it can be thought of as a function acception 3 parameters: a condition of type Boolean, an expression of Some type A to return in case the condition is true, and another expression of the same type A to return if the condition is false. This is nonstrict since it won't evaluate all of its args. *It is strict in its condition parameter and nonstrict in the two branches for the true and false cases.*

In Scala, we can write *nonstrict* functions by accepting some of our arguments unevaluated. Type `() => A` is a function that accepts zero arguments and returns an `A`. In fact, it is a syntactic alias for the type `Function0[A]`. In general, the unevaluated form of an expression is called a **thunk**, and we can force the thunk to evaluate the expression and get the result. We do so by invoking the function and passing an empty argument list.

Overall, this syntax makes it very clear what’s happening; we’re passing a function of no arguments in place of each nonstrict parameter and then explicitly calling this function to obtain a result in the body (we can call it > 1 times as well). But scala provdies a nicer syntax as seen below in `betterIf`.

The type `=> A` allows us to be succint. We don't need to do anything special to evaluate except just reference it. We say that a nonstrict function in Scala takes its arguments **by name** rather than by value.

```scala
// Without using the nicer syntax
def if[A](cond: Boolean, onTrue: () => A, onFalse: () => A): A = 
    if cond then onTrue() else onFalse()

def betterIf[A](cond: Boolean, onTrue: => A, onFalse: => A): A =
    if cond then onTrue else onFalse

if(a < 22, () => println("a"), () => println("b"))
betterIf(a < 22, println("a"), println("b"))
```

With either syntax, an argument that’s passed unevaluated to a function will be evaluated once for each place it’s referenced in the body of the function. **Scala won't by default cache the result of evaluation!** We can cache the value explicitly if we wish to only evaluate the result once by using the `lazy` keyword.

```scala
def maybeTwice(b: Boolean, i: => Int): Int = 
    if b then i + i else 0
val x = maybeTwice(true, { println("hi"); 1 + 41})
// hi
// hi
// x: Int = 84

def maybeTwice2(b: Boolean, i: => Int): Int = 
    lazy val j = i
    if b then j + j else 0
val x = maybeTwice2(true, { println("hi"); 1 + 41 })
// hi
// x: Int = 84
```

Adding a **`lazy`** keyword to a val declaration will cause Scala to delay evaluation of the right-hand side of that lazy val declaration until its first reference during evaluation of another expression. It will also **cache the result** so subsequent references to it don’t trigger repeated evaluation.

**Formal defintion of strictness**: If the evaluation of an expression runs forever or throws an error instead of returning a definite value, we say the expression doesn't *terminate* or it evaluates to *bottom*. A function f is **strict** if the expression f(x) evaluates to bottom for all X that evaluate to bottom. 

**My defintion**: A strict function evaluates its input before doing anything else. Therefore, if it receives a broken or non-terminating input, the function itself is doomed to fail in the exact same way.

## Lazy lists: An extended example (look at datastructures/LazyList.scala)

We’ll see how chains of transformations on lazy lists are fused into a single pass by using laziness.

```scala
enum LazyList[+A]:
  case Empty
  case Cons(h: () => A, t: () => LazyList[A])

object LazyList:
...
```

This looks identical to `List` except the `Cons` data constructor takes *explicit* thunks (`() => A` and () => `LazyList[A]`), instead of strict values. Remeber **thunk means the unevaluated form of an expression**. We use explicit thunks here because **Scala doesn’t allow the parameters of a case class to be by-name parameters.** This limitation is the result of each parameter of a case class getting a corresponding public val.

If we wish to examine or traverse the `LazyList`, we need to force these thunks! Heres an example:

```scala
/** optionall extract the head of a LazyList */
def headOption: Option[A] = this match
    case Empty => None
    case Cons(h, _) => Some(h()) // forcing of the the h thunk using h()
```

This ability of `LazyList` to evaluate only the portion actually demanded (we don't evaluate the tail of the Cons) is useful, as we will see!

### Memoizing lazy lists and avoiding recomputation

We typically want to cache the values of a `Cons` node once they are forced. If we use the `Cons` data constructor directly, for instance, this code will actually **compute `expensive(x)` twice.**

```scala
val x = Cons(() => expensive(x), tl)
val h1 = x.headOption
val h2 = x.headOption
```

We typically avoid this problem by defining *smart constructors*. Here our smart constructor takes care of memoizing the by-name arguments for the head and tail of the `Cons`. **This is a common trick and ensures our thunk will only do its work once when forced for the first time.** Subsequent forces will return the cached *lazy val*.

```scala
def cons[A](hd: => A, tl: => LazyList[A]): LazyList[A] =
   lazy val head = hd
   lazy val tail = tl
   Cons(() => head, () => tail)

// annotates Empty as a LazyList[A], which is better for type inference in some cases. 
// Read Smart Constructors section below in Misc Notes
def empty[A]: LazyList = Empty
```

```scala
def apply[A](as: A*): LazyList[A] =
  if as.isEmpty then empty
  else cons(as.head, apply(as.tail*))
```

Each time the head thunk is referenced in the resulting LazyList, the value of the lazy val head is returned. If that lazy val has already been initialized, then its cached value is returned. Otherwise, it’s computed, cached, and returned. This smart constructor gives us the best of all worlds; there is no need to manually create thunks at the call site of `Cons` or to cache the result, so it’s only computed once, and we retain the features provided by case classes.

Scala takes care of wrapping the arguments to cons in thunks, so the `as.head` and `apply(as.tail*)` expressions won’t be evaluated until we force the LazyList.

The `as` argument to apply is strict, however! When apply is called, each individual `A` expression is evaluated before the definition of `apply` is evaluated. To defer evaluation of each argument until forced by the resulting LazyList, we’d need each `A` to be by-name:

```scala
def apply[A](as: (=> A)*): LazyList[A] = ???
```
But scala doesn't support this syntax. We will discuss other ways of **lazily constructing a LazyList** later in this chapter.

## Separating program description from evaluation

A major theme in functional programming is **separation of concerns**. We want to **separate the description of computations from actually running them.** Examples:

- `first-class functions` capture some computation in their bodies but only execute it once they receive their arguments.
- `Option` to capture the fact that an error occurred, where the decision of what to do became a separate concern. 
- `LazyList`, we’re able to build up a computation that produces a sequence of elements without running the steps of that computation until we need those elements.

**NOTE: Laziness lets us separate the description of an expression from the evaluation of that expression. **

We may choose to describe a larger expression than we need and then evaluate only a portion of it. Example, lets look at function `exists` that checks whether an element matching a `Boolean` function exists in this `LazyList`:

```scala
def exists(p: A => Boolean): Boolean = this match
    case Cons(h, t) => p(h()) || t().exists(p)
    case _          => false
```

Note that `||` is nonstrict in its second argument. If `p(h())` returns true, then exists terminates the traversal early and returns true as well. Remember also that the tail of the lazy list is a lazy val, so **not only does the traversal terminate early, but the tail of the lazy list is never evaluated at all! So whatever code would have generated the tail is never actually executed.**

#### Looking at implementation of `map`, `flatMap`, `foldRight`... in `LazyList`

These implementaions are incremental. They don't fully generate their answers. It's not until some other computation looks at the elements of the resulting `LazyList` that the computation to generate that `LazyList` actually takes place - and then it will do just enough work to generate the requestd elements. Because of this incremental nature, we can call these functions one after another without fully instantiating the intermediate results. 

Let’s look at a simplified program trace for (a fragment of) the motivating example we started this chapter with. We’ll convert this expression to a `List` to force evaluation. Take a minute to work through this trace to understand what’s happening. Remember that a trace like this is just the same expression repeated multiple times, evaluated by one more step each time.

```scala
LazyList(1, 2, 3, 4).map(_ + 10).filter(_ % 2 == 0).toList
cons(11, LazyList(2, 3, 4).map(_ + 10)).filter(_ % 2 == 0).toList   // #1
LazyList(2, 3, 4).map(_ + 10).filter(_ % 2 == 0).toList             // #2
cons(12, LazyList(3, 4).map(_ + 10)).filter(_ % 2 == 0).toList      // #3
12 :: LazyList(3, 4).map(_ + 10).filter(_ % 2 == 0).toList          // #4
12 :: cons(13, LazyList(4).map(_ + 10)).filter(_ % 2 == 0).toList
12 :: LazyList(4).map(_ + 10).filter(_ % 2 == 0).toList
12 :: cons(14, LazyList().map(_ + 10)).filter(_ % 2 == 0).toList
12 :: 14 :: LazyList().map(_ + 10).filter(_ % 2 == 0).toList        // #5
12 :: 14 :: List()                                                  // #6

// #1 Apply map to the first element.
// #2 Apply filter to the first element.
// #3 Apply map to the second element.
// #4 Apply filter to the second element. Produce the first element of the result.
// #5 Apply filter to the fourth element, and produce the final element of the result.
// #6 map and filter have no more work to do, and the empty lazy list becomes the empty list.
```

Notice in this trace is **how the filter and map transformations are interleaved**—the computation alternates between generating a single element of the output of map and testing with filter to see if that element is divisible by 2 (adding it to the output list if it is). **Note that we don’t fully instantiate the intermediate lazy list that results from the map. It’s exactly as if we had interleaved the logic using a special-purpose loop**. 

For this reason, people sometimes describe lazy lists as **first-class loops whose logic can be combined using higher-order functions, like map and filter.** That is a **core philosophy of Functional Programming — treating data as a stream and transformations as the control flow**. By using lazy lists (also called Streams or Generators), you effectively decouple the definition of a loop from its execution. This concept describes functional iteration, where **loops are treated as data structures (lazy lists or streams) rather than control flow statements**. Instead of manually managing a loop's state with for or while, you define what should happen to the data using a pipeline of transformations.

## Misc Notes 

### Smart Constructors

Smart constructors are functions for constructing data types that ensure some additional invariant or provide a slightly different signature than the real constructors. By convention, they are typically lowercase the first letter of the corresponding data constructor. E.g for `Cons(...)` it would be `def cons(...)`.

Recall that Scala uses subtyping to represent data constructors, but we almost always want to infer `LazyList` as the type, not `Cons` or `Empty`. Making smart constructors that return the base type is a common trick, though one that was more important in Scala 2 than Scala 3, as Scala 3 will generally prefer to infer the type of the enum (e.g., `LazyList[A]`) instead of the type of the data constructor (e.g., `Cons[A]`).] We can see how both smart constructors are used in the `LazyList.apply` function.
