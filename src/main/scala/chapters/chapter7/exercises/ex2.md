## Exercise 7.2

Before continuing, try to come up with representations for `Par` that make it possible to implement the functions of our API.

## Solution

Par represents a description of a parallel computation. We know `run` needs to execute asynchronous tasks somehow. We could write our own low-level API, but there is already a class we can use in the `Java` std library. Here is a simple excerpt:

```scala
class ExecutorService:
  def submit[A](a: Callable[A]): Future[A]
 
trait Callable[A]:
  def call: A         // Essentially just a lazy A
 
trait Future[A]:
  def get: A
  def get(timeout: Long, unit: TimeUnit): A
  def cancel(evenIfRunning: Boolean): Boolean
  def isDone: Boolean
  def isCancelled: Boolean
```

So `ExecutorService` lets us submit a `Callable` value (in Scala we’d probably just use a lazy argument to submit) and get back a corresponding `Future` that’s a handle to a computation that’s potentially running in a separate thread. We can obtain a value from a Future with its `get` method (which blocks the current thread until the value is available), and it has some extra features for cancellation (throwing an exception after blocking for a certain amount of time and so on).

Let’s try assuming our `run` function has access to an `ExecutorService` and see if that suggests anything about the representation for `Par`. The simplest possible model for `Par[A]` might be `ExecutorService => A`. This would make run trivial to implement. But it might be nice to defer the decision of how long to wait for a computation, or whether to cancel it, to the caller of run. So `Par[A]` becomes `ExecutorService => Future[A]`, and run simply returns the Future:


## SOLUTION CODE 

```scala
// Using opaque type provides encapsulation of internals & avoids unnecessary allocations.
opaque type Par[A] = ExecutorService => Future[A]
extension [A](pa: Par[A]) def run(s: ExecutorService): Future[A] = pa(s)
```

Note that since `Par` is represented by a function that needs an `ExecutorService`, the creation of the `Future` doesn’t actually happen until this `ExecutorService` is provided. Is it really that simple? Let’s assume it is for now and revise our model if we find it doesn’t allow some functionality we’d like.

