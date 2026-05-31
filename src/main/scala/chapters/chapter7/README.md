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

Observe that we’re no longer calling `unit` in the recursive case, and it isn’t clear whether `unit` should accept its argument lazily anymore. In this example, accepting the argument lazily doesn’t seem to provide any benefit, but perhaps this isn’t always the case. Let’s come back to this question later.

What about `map2`-should it takes its arguments lazily? It would make sense for map2 to run both sides of the computation in parallel, giving each side equal opportunity to run (it would seem arbitrary for the order of the map2 args to matter, since we simply want map2 to indicate that the two computations being combined are independent and can be run in parallel). 

What choice lets us implement this? As a simple test case, consider what happens if `map2` is strict both arguments and we're evaluating `sum(IndexSeq(1,2,3,4))`. 

```scala
sum(IndexedSeq(1,2,3,4))

map2(sum(IndexedSeq(1,2)), sum(IndexedSeq(3,4)))(_ + _)

map2(
  map2(
    sum(IndexedSeq(1)),
    sum(IndexedSeq(2)))(_ + _),
    sum(IndexedSeq(3,4)))(_ + _)

map2(
  map2(
    unit(1),
    unit(2))(_ + _),
  sum(IndexedSeq(3,4)))(_ + _)

map2(
  map2(
    unit(1),
    unit(2))(_ + _),
  map2(
    sum(IndexedSeq(3)),
    sum(IndexedSeq(4)))(_ + _))(_ + _)
...
```

Because `map2` is strict and Scala evaluates arguments left to right, whenever we encounter `map2(sum(x), sum(y))(_ + _)`, we must evaluate `sum(x)` and so on recursively. This has the rather unfortunate consequence of requiring us to strictly construct the entire left half of the tree of summations first before moving on to (strictly) constructing the right half. 

Here `sum(IndexedSeq(1,2))` gets fully expanded before we consider `sum(IndexedSeq(3,4))`. And if map2 evaluates its arguments in parallel (using whatever resource is being used to implement the parallelism, like a thread pool), that implies the left half of our computation will start executing before we even begin constructing the right half of our computation.

What if we keep `map2` strict but don’t have it begin execution immediately? Does this help? If map2 doesn’t begin evaluation immediately, this implies a `Par` value is merely constructing a description of what needs to be computed in parallel. Nothing actually occurs until we evaluate this description, perhaps using a `get`-like function. The problem is that if we construct our descriptions strictly, they’ll be rather heavyweight objects. Looking back at our trace, our description will have to contain the full tree of operations to be performed:

```scala
map2(
  map2(
    unit(1),
    unit(2))(_ + _),
  map2(
    unit(3),
    unit(4))(_ + _))(_ + _)
```

No matter what data structure we use to store this description, it’ll likely occupy more space than the original itself. It would be nice if our descriptions were more lightweight.

We should make `map2` lazy and have it begin immediate execution of both sides in parallel. This also addresses the problem of giving neither side priority over the other.

## Explicit forking

Something still doesn't feel right. Is it *always* the case that we want to evaluate the two arguments to `map2` in parallel? Probably not. Consider the following example

```scala
Par.map2(Par.unit(1), Par.unit(1))(_ + _)
```

In this case, there isn't much point in spawning off a separate logical thread to evaluate them, but our API doesn't give us any way of providing this sort of info. Our current API is very inexplict about when computations get forked off the main thread; the programmer doesn't get to specify where this forking should occur. 

What if we make the forking more explicit? We can do that by inventing another function `fork`:

```scala
// means that the given Par should be run in a separate logical thread
def fork[A](a: => Par[A]): Par[A]

def sum(ints: IndexedSeq[A]): Par[Int] = 
  if ints.size <= 1 then
    Par.unit(ints.headOption.getOrElse(0))
  else
    val (l, r) = ints.splitAt(ints / 2)
    Par.map2(Par.fork(sum(l)), Par.fork(sum(r)))(_ + _)
```

**With `fork` we can now make `map2` strict, leaving it up to the programmer to wrap arguments if they wish**. A function like `fork` solves the problem of instantiating our parallel computations too strictly, but more fundamentally, it puts the parallelism explicitly under programmer control. We're addressing two concerns here:

1. We need some way to indicate that the results of the two parallel tasks should be combined
2. We have the choice of whether a particular task should be performed asynchronously.

By keeping these concerns separate, we avoid having any sort of global policy for parallelism attached to `map2` and other operations we write.

Lets now return to the question of whether `unit` should be strict or lazy. With `fork` we can now make `unit` strict without any loss of expressiveness. A nonstrict version of it—let’s call it lazyUnit—can be implemented using `unit` and `fork`. Thats the power of composition. `lazyUnit` is a simple example of a derived combinator, as opposed to a primitive combnator, like `Unit`.

We were able to define `lazyUnit` in terms of other operations; later, when we pick a representation for Par, `lazyUnit` won’t need to know anything about this representation—its only knowledge of `Par` will come from the operations `fork` and `unit` that are defined on Par

```scala
def unit[A](a: A): Par[A]
def lazyUnit[A](a: => A): Par[A] = fork(unit(a))
```

We know we want `fork` to **signal that its argument gets evaluated in a separate logical thread**, but we still have the question of whether it should begin doing so immediately upon being called or hold on to its argument to be evaluated in a logical thread later when the computation is forced, using something like `get`. Should evaluation be eager or lazy?

#### Should evaluation be the responsiblity of fork or get?

If `fork` begins evaluating its argument immediately in parallel:

- the implementation must clearly know something, either directly or indirectly, about how to create threads or submit tasks to some sort of thread pool. 
- implies that the thread pool (or whatever resource we use to implement the parallelism) must be (globally) accessible and properly initialized wherever we want to call `fork`. (This is much like how the credit card processing system was accessible to the buyCoffee method in our Cafe example in chapter 1.)
- This means we lose the ability to control the parallelism strategy used for different parts of our program.

While there is nothing inherently wrong with having a global resource for executing parallel tasks, we can imagine how it would be useful to have more fine-grained control over what implementations are used where (we might want each subsystem of a large app to get its own thread pool with different parameters for example). **It seems much more appropriate to give `get` the responsibility of creating threads and submitting executing tasks.** 

Note that coming to these conclusions didn’t require knowing exactly how fork and get will be implemented or even what the representation of `Par` will be. We just reasoned informally about the sort of information required to actually spawn a parallel task and examined the consequences of having `Par` values know this information.

If `fork` holds onto its unevaluated argument until later:

- It requires no access to the mechanism for implementing parallelism; it just takes an unevaluated `Par` and **marks it for concurrent evaluation**.

Lets assume fork is lazy. With this model, `Par` itself doesn't need to know how to actually implement the parallelism. It's more a description of a parallel computation that gets interpreted at a later time by something like the `get` function. This is a shift from before, where we were considering Par to be a container of a value that we could simply get when it becomes available. Now its more of a first-class program we can run. So lets rename our `get` function to `run` and dictate that this where the parallelism actually gets implemented.

Because **`Par` is now just a pure data structure, `run` needs to have some means of implementing the parallelism**, whether it spawns new threads, delegates tasks to a thread pool, or uses some other mechanism.

```scala
extension [A](pa: Par[A]):
  def run: A
```

## Picking a representation

We've sketched out the follwing api

```scala
def unit[A](a: A): Par[A]                             // 1
extension [A](pa: Par[A])
  def map2[B, C](pb: Par[B])(f: (A, B) => C): Par[C]  // 2
  def run: A                                          // 3
def fork[A](a: => Par[A]): Par[A]                     // 4
def lazyUnit[A](a: => A): Par[A] = fork(unit(a))      // 5
```


- `unit`: Promotes a constant value to a parallel computation
- `map2`: Combines the results of two parallel computations with a binary function
- `fork`: Marks a computation for concurrent evaluation—the evaluation won’t occur until forced by run.
- `lazyUnit`: Wraps its unevaluated argument in a Par and marks it for concurrent evaluation
- `run`: Extracts a value from a Par by performing the computation

Look at exercise 2 in chapter7/exercises/ex2.md and its solution to see a first initial attempt of reprentation. We come into an initial type conclusion for `Par` as:

```scala
opaque type Par[A] = ExecutorService => Future[A]
extension [a](pa: Par[A])
  def run(s: ExecutorService): Future[A] = pa(s)
```

## Refining the API

We should note that `Future` doesn’t have a purely functional interface. This is part of the reason we don’t want users of our library to deal with Future directly. But importantly, even though methods on `Future` rely on side effects, our entire `Par` API remains pure. **It’s only after the user calls `run` and the implementation receives an `ExecutorService` that we expose the Future machinery**. Our users, therefore, program to a pure interface whose implementation nevertheless relies on effects at the end of the day. **But since our API remains pure, these effects aren’t side effects**. In part 4, we’ll discuss this distinction in detail.

What else can we express with our existing combinators? (`map2`, `lazyUnit`, `fork`, `asyncF`, `unit` and `run`). Lets look at another concrete example. 

Suppose we have a `Par[List[Int]]` representing a parallel computation that produces a `List[Int]`, and we'd like to convert this to a `Par[List[Int]]` whose result is sorted:

We could run the Par, sort the resulting list and repackage it in a `Par` with `Unit` -- but we want to avoid calling run. The only other combinator we have that allows ut to manipulat the value of `Par` in a way is `map2`. So if we passed parList to one side of map2, we'd be able to gain access to the `List` inside and sort it, and we can pass whatever we want to the other side of `map2`, so lets just pass a no-op.

```scala
def sortPar(parList: Par[List[Int]]): Par[List[Int]]
  parList.map2(unit(()))((a, _) => a.sorted)
```

We can now tell a `Par[List[Int]]` that we’d like that list sorted, but we might as well generalize this further. We can lift any function of type `A => B` to become a function that takes `Par[A]` and returns `Par[B]`, and we can map any function over a `Par`:

```scala
extension [A](pa: Par[A]) def map[B](f: A => B): Par[B] =
  pa.map2(unit(()))((a, _) => f(a))

def sortPar(parList: Par[List[Int]]) =
  parList.map(_.sorted)
```

What else can we implement using our API? Could we map over a list in parallel? Unlike `map2`, which combines two parallel computations, `parMap` needs to combine N parallel computations. It seems like this should somehow be expressable:

```scala
def parMap[A, B](ps: List[A])(f: A => B): Par[List[B]]
```

We could just write `parMap` as a new primitive instead. Remember `Par[A]` is simply an alias for `ExecutorService => Future[A]`. There is nothing wrong with implementing operations as new primitives. In some cases, we can even implement the operations more efficiently by assuming something about the underlying representation of the data types we're working with. 

But right now, we are just interested in exploring what operations are expressible using our existing API and grasping the r/n b/n the various operations we've defined. **Understanding what combinators are truly primitive will become more important** in part3 when we show how to abstract over common patterns across libraries.

In this case, there’s another good reason *not to implement `parMap` as a new primitive*: it’s challenging to do so correctly, particularly if we want to properly respect timeouts. It’s frequently the case that primitive combinators encapsulate some rather tricky logic, and reusing them means we don’t have to duplicate this logic.

```scala
def parMap[A,B](ps: List[A])(f: A => B): Par[List[B]] =
  val fbs: List[Par[B]] = ps.map(asyncF(f))
```

Remember that `asyncF` converts an `A => B` to an `A => Par[B]` by *forking a parallel computation* to produce the result. So we can fork off our N parallel computations pretty easily, but we need some way of collecting their results. Are we stuck? Well, just from inspecting the types, we can see that we need some way of converting our `List[Par[B]]` to the `Par[List[B]]` required by the return type of `parMap`. We need a `sequence` function then we can complete our impl of `parMap` 

```scala
def sequence[A](ps: List[Par[A]]): Par[List[A]] =
  ps.foldRight(unit(Nil)) {
    case (parA, parList) => parA.map2(parList)((a, la) => a :: la)
  }

def parMap[A, B](ps: List[A])(f: A => B): Par[List[B]] =
  fork {
    val fbs: List[Par[B]] = ps.map(asyncF(f))
    sequence(fbs)
  }
```

With this implementation, `parMap` will return immediately, even for a huge input list. When we later call `run`, it will `fork` a single asynchronous computation, which itself spawns N parallel computations and then waits for these computations to finish, collecting their results into a list. If, instead, we left out the call to `fork`, calling parMap would first create the `fbs` list before calling `sequence`, resulting in performing some of the computation on the calling thread.

Another example: 

```scala
def parFilter[A](as: List[A])(f: A => Boolean): Par[List[A]] =
  fork:                                                     // 1
    val pars: List[Par[List[A]]] =
      as.map(asyncF(a => if f(a) then List(a) else Nil))    // 2
    sequence(pars).map(_.flatten)                           // 3

// 1. Like in parMap, we fork immediately, so the mapping over the original list is done on a separate 
//    logical thread rather than the caller’s thread.

// 2. We use asyncF to convert our A => List[A] function to an A => Par[List[A]] function.
// 3. sequence(pars) returns a `Par[List[List[A]]]`, so we map over that and flatten the inner nested lists.
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

### ExecutorService and Future

`ExecutorService` is a core interface in Java in the `java.util.conucrrent` package that provides a higher-level replacement for using raw threads. It manages a pool of threads to handle asynchronous tasks more efficiently, without the need to manually create or manage individual threads. It handles creation, scheduling, and reuse, significantly improving performance compared to manually managing Thread instances

```java
import java.util.concurrent.*;
public class ExecutorExample {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(3);  // Pool of 3 threads// Submit tasks to the executor
        executorService.submit(() -> System.out.println("Task 1 is running"));
        executorService.submit(() -> System.out.println("Task 2 is running"));
        executorService.submit(() -> System.out.println("Task 3 is running"));        
        executorService.shutdown();  // Shutdown the executor
    }
}
```

In Java/Scala, a `Future` represents the pending result of an asynchronous computation. It serves as a read-only placeholder for a value that is being computed by another thread. When you submit a task to a thread pool via an `ExecutorService`, you immediately receive a `Future` object to track, manage, or retrieve that task's eventual output.

The `Java` Future interface provides five essential methods to manage an asynchronous task:

- `get()`: Blocks the executing thread until the computation completes and then retrieves the result.
- `get(long timeout, TimeUnit unit)`: Blocks for a specified timeframe to retrieve the result. It throws a `TimeoutException` if the task fails to finish on time.
- `cancel(boolean mayInterruptIfRunning)`: Attempts to abort task execution. The parameter decides whether to interrupt the thread if the task already started.
- `isDone()`: Returns true if the task completed, failed, or was canceled.
- `isCancelled()`: Returns true if the task was successfully aborted before normal completion.

```java
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class FutureExample {
    public static void main(String[] args) {
        // 1. Create a thread pool
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // 2. Define a task that returns a result
        Callable<Integer> task = () -> {
            TimeUnit.SECONDS.sleep(2); // Simulate long-running work
            return 42;
        };

        System.out.println("Submitting task...");
        // 3. Submit the task to get a Future handle
        Future<Integer> future = executor.submit(task);

        System.out.println("Doing other work in main thread...");

        try {
            // 4. Retrieve the result (this blocks until the 2 seconds finish)
            Integer result = future.get(); 
            System.out.println("Result received: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 5. Always shut down your executor service
            executor.shutdown();
        }
    }
}
```

### @volatile

The `@volatile` annotation in Scala ensures that changes to a variable are immediately visible to other threads. When applied to a mutable field (var), it prevents threads from caching the value locally, forcing them to always read and write directly to the main memory. It must be applied to a `var`. Applying it an immutable `val` serves no purpose and will result in compiler warning.


