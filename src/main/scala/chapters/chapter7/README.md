# (Start of Part 2)

## Brief intro to Part 2

In part 2, we’ll see how the assumptions of FP we have built in part 1 affect library design. We’ll create three useful libraries in part 2 for:

- parallel and asynchronous computation
- testing programs
- parsing text.

The primary goal is developing skills in designing functional libraries, even for domains that look nothing like the ones here. There are no strict right answers in functional library design. Instead, we have a collection of design choices, each with different trade-offs. The goal is to understand these trade-offs and what different choices mean. 

Library design is not something only a select few people get to do; it’s part of the day-to-day work of ordinary functional programming. **In these chapters and beyond, absolutely feel free to experiment, play with different design choices, and develop your own aesthetic.**

As a final note, there are some repeated patterns of similar-looking code. Keep this in the back of mind. Part 3 covers how to remove this duplication, and discover an entire world of fundamental abstractions common to all libraries.

---

# Chapter 7

This chapter covers Purely functional parallelism

## Topics

- Developing a functional API for parallel computations
- Approaching APIs algebraically
- Defining generic combinators

The interaction of programs that run with parallelism is complex, and the traditional mechanism for communication among execution threads—*shared mutable memory*—is notoriously difficult to reason about. This can all too easily result in programs that have race conditions and deadlocks, aren’t readily testable, and don’t scale well.

Main concern will be making our library highly composable and modular. To this end, we’ll keep to our theme of **separating the concern of describing a computation from actually running it.** We want to allow users of library to write programs at a very high level, insulating them from the nitty-gritty of how their programs will be executed. Example:

```scala
// parMap lets us apply a function, f, to every element in a collection simulatneously
val outputList = parMap(inputList)(f)
```

We will emphasize **algebraic reasoning** and introduce the idea that an API can be described by *an algebra* that obeys specific laws. 

Why design our own library? Why not just use the concurrency primitives that come with Scala’s standard library in the `scala.concurrent` package? This is because

- Showing how easy it is to design your own practical libraries
- Encourage the view that no existing library is authoritative or beyond reexamination, even if designed by experts and labeled *standard*.

When starting from scratch, we get to revisit all the fundamental assumptions that went into designing the library, take a different path, and discover things about the problem space that others may not have even considered. As a result, we might arrive at our own design that suits our purposes better. In this case, **our fundamental assumption will be that our library admits absolutely no side effects.**

## Choosing data types and functions

Difficulty in designing a function library comes from refining ideas and finding a data type that enables the functionality we want. We'd like to be able to **create parallele computations**!

Lets first examine a simple, parallelizable computation:

```scala
def sum(ints: Seq[Int]): Int = 
  ints.foldLeft(0)((a, b) => a + b)
```

Instead of folding, we could use a *divide-and-conquer* algorithm

```scala
def sum(ints: IndexedSeq[Int]): Int =
  if ints.size <= 1 then
    ints.headOption.getOrElse(0)
  else
    val (l, r) = ints.splitAt(ints.size / 2)
    sum(l) + sum(r)
```

- The divide and conquer impl can be parallelized; the two halves can be summed in parallel. 
- The foldLeft version can't be parallelized. 

Lets shift our perspective. Rather than focusing on how this parallelism will ultimately be implemented and forcing ourselves to work with the implementation APIs directly (likely related to `java.lang.Thread` and the `java.util.concurrent` library), we'll instead design our own ideal API, and work from there to an impl.

## Data type for parallel computations

Any data type we might choose to represent our parallel computations needs to:

- Able to contain a result (result will have some meaningful type, ex `Int` for the sum example)
- Able to extract this result.

Lets apply this knowledge to our design. For now, we can just invent a container type for our result, `Par[A]`. The functions we need are:

```scala
/** Takes an unevaluated A, returning a computation that might evaluate it in a separate thread.
  * We call it unit because, in a sense, it creates a unit of parallelism that just wraps a
  * single value.
  */
def unit[A](a: => A): Par[A]
/** extracts the resulting value from parallel computation */
def get[A](a: Par[A]): A
```

For now, we don’t need to worry about what other functions we require, what the internal representation of `Par` might be, or how these functions are implemented.

```scala
// We’ve wrapped the two recursive calls to sum in calls to unit, and 
// we’re calling get to extract the two results from the two subcomputations.
def sum(ints: IndexedSeq[Int]): Int = 
  if ints.size <= 1 then
    ints.headOption.getOrElse(0)
  else
    val (l, r) = ints.splitAt(ints.size / 2)
    val sumL: Par[Int] = Par.unit(sum(l)) // computes leftHalf in parallel
    val sumR: Par[Int] = Par.unit(sum(r)) // computes rightHalf in parallel
    Par.get(sumL) + Par.get(sumR)         // Extract both results and sum
```

We now have a choice about the meaning of `unit` and `get`. `unit` could begin evaluating its argument immediately in a separate (logical) thread, or it could simply hold onto its argument until `get` is called and begin evaluation then. But in this example, if we want to obtain any degree of parallelism, we require `unit` to being evaluating its argument concurrently and return immediately. Why?

**Because** function arguments in Scala are strictly evaluated from left to right, so if `unit` delays execution until `get` is called, then on last line it will spawn the parallel computation for left and wait for it to finish before spawning the second parallel computation. This means the computation is effectively sequential!

```
        What we want

time --->

left:   [computing---------done]
right:  [computing---------done]
    
        What happens if unit delays until get is called

time --->

left:   [computing---------done]
right:                         [computing---------done]
```

But if `unit` begins evaluating its argument concurrently, then calling `get` arguably breaks referential transparency. We can see this by replacing `sumL` and `sumR` with their definitions; if we do so, we still get the same result, but **our program is no longer parallel**:

```scala
Par.get(Par.unit(sum(l))) + Par.get(Par.unit(sum(r)))
```

If `unit` starts evaluating its argument right away, `get` will wait for that evaluation to complete. This means the two sides of the + sign won’t run in parallel if we simply inline the `sumL` and `sumR` variables. We can see that `unit` has a **definite side effect** but only with regard to `get`. `unit` simply returns a `Par[Int`] in this case, representing an asynchronous computation. But as soon as we pass that `Par` to `get`, we explicitly wait for it, exposing the side effect. So it seems we want to avoid calling get—or at least delay calling it until the very end. We want to be able to combine asynchronous computations without waiting for them to finish.

Currently, once you call `get`, you're no longer building a parallel computation—you are running it and waiting. We want combinators that combine `Par`s without extracting their values yet. Instead of `Int + Int` we want `Par[Int] + Par[Int] => Par[Int]`. We want to call get at the very end once we done our description:

```scala
// Example: allows the runtime to evaluate both sides concurrently before synchronizing.
val combined = Par.map2(sumL, sumR)(_ + _)
Par.get(combined)
```

### Combining parallel computations

Let’s see if we can avoid the aforementioned pitfall of combining unit and get. If we don’t call `get`, that implies that our sum function must return a `Par[Int]`. What consequences does this change reveal?

```scala
def sum(ints: IndexedSeq[Int]): Par[Int] = 
  if ints.size <= 1 then
    Par.unit(ints.headOption.getOrElse(0))
  else
    val (l, r) = ints.splitAt(ints.size / 2)
    Par.map2(sum(l), sum(r))(_ + _)
```

## Misc Notes

### Problem with using concurrency primitives directly

Why not use `java.lang.Thread` and `Runnable`? Lets take a look at these classes in partial excerpt in scala.

```scala
trait Runnable:
  def run: Unit

class Thread(r: Runnable):
  def start: Unit // Begins running r in a separate thread
  def join: Unit // Blocks the calling thread until r finishes running.
```

We can already see a problem with both of these types: **none of the methods return a meaningful value**. Therefore, if we want to get any information out of a `Runnable` it has to have some side effect, like mutating some state we can inspect.

This is bad for compositionality; we can’t manipulate `Runnable` objects generically since we always need to know something about their internal behavior. `Thread` also has the disadvantage that it maps directly onto operating system threads, which are a scarce resource. It would be preferable to create as many logical threads as is natural for our problem and deal with mapping these onto actual OS threads later.

The above problem can be handled by something like `java.util.concurrent.Future,ExecutorService` or similar. Why don't we use them directly? Again a portion of their API:

```scala
class ExecutorService:
  def submit[A](a: Callable[A]): Future[A]

trait Future[A]:
  def get: A
```

While they are a tremendous help in abstracting over physical threads, these are still at a much lower level of abstraction than the library we want to create in this chapter. A call to `Future.get`, for example, blocks the calling thread until the `ExecutorService` has finished executing it, and its API provides no means of composing futures. Of course, we can build the impl of our library on top of these tools (and we will do this later in the chapter), but they don't present a modular and compositional API that we'd want to use directly from functional programs.

### Logical threads

The term *logical thread* informally means **a computation that runs concurrently with the main execution thread of our program**. There need not be a one-to-one correspondence between logical threads and OS/physical threads; we may have a large number of logical threads mapped onto a smaller number of OS threads via thread pooling, for instance.

### Importance of simple examples

Summing integers is, in practice, probably so fast that parallelization imposes more overhead than it saves. Complicated examples include all sorts of incidental structure and extraneous detail that can confuse the initial design process. We’re trying to explain the essence of the problem domain, and a good way of doing this is by starting with trivial examples, factoring out common concerns across these examples, and gradually adding complexity. In functional design, **our goal is achieving expressiveness not with mountains of special cases but by building a simple and composable set of core data types and functions.**

### IndexedSeq

A superclass of random-acess sequences, like Vector. Unlike lists, these sequences provide an efficient splitAt method for dividing them into two parts at a particular index.

### headOption

A safe, idiomatic method used to retrieve the first element of a collection. It is defined on all collections in Scala. Doesn't throw an excetion like the standard `head` method does on empty collections.
