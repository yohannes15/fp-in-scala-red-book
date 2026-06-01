## Exercise 7.7

### Question 1

**Hard**: Given `y.map(id) == y`, `y.map(g).map(f) == y.map(f compose g)` is a free theorem. (This is sometimes called map fusion, and it can be used as an optimization; rather than spawning a separate parallel computation to compute the second mapping, we can fold it into the first mapping.) Can you prove it?

### Question 2

Our representation of `Par` doesn’t give us the ability to implement this optimization, since it’s an opaque function. For example, `y.map(g)` returns a new `Par` that’s a black box—when we then call `.map(f)` on that result, we’ve lost the knowledge of the parts that were used to construct `y.map(g)`: namely, `y`, `map`, and `g`. All we see is the opaque function and, hence, cannot extract out `g` to compose with `f`. 

If `Par` was written as a data type (e.g., an enumeration of various operations), then we could pattern match and discover opportunities to apply this rule. You may want to try experimenting with this idea on your own.

### Solution 1

```scala
y.map(id) == y // given thereom
--------------------------------------------------------------------
y.map(g).map(f) == y.map(f compose g)                           //1
y.map(id).map(f) == y.map(f compose id)                         //2
y.map(id).map(f compose g) == y.map((f compose g) compose id)   //3
y.map(id).map(f compose g) == y.map(f compose g)                //4
y.map(f compose g) == y.map(f compose g)                        //5
```

1. Initial law
2. Substitue the `identity` function for `g`. 
3. Substitue `f compose g` for `f`
4. Simplify right hand side to `f compose g`. **x compose id is x**
5. Simplify `y.map(id)` to `y`

### Solution 2

If we represent `Par` as a data type instead of an opaque function, we can pattern match in `map` and fuse:

```scala
enum Par[+A]:
  case Unit(a: A)
  case Map[A, B](source: Par[A], f: A => B) extends Par[B]
  case Map2[A, B, C](pa: Par[A], pb: Par[B], f: (A, B) => C) extends Par[C]
  case Fork[A](source: () => Par[A]) extends Par[A]

  def map[B](f: A => B): Par[B] = this match
    case Map(source, g) => Map(source, g andThen f) // FUSE!
    case other          => Map(other, f)

  def run(es: ExecutorService): A = this match
    case Unit(a)        => a
    case Map(source, f) => f(source.run(es))
    case Fork(src)      => es.submit(new Callable[A] {
      def call = src().run(es)
    }).get
    case Map2(pa, pb, f) =>
      val a = pa.run(es)
      val b = pb.run(es)
      f(a, b)
```

```scala
import java.util.concurrent.*

// Concrete example:
val es = Executors.newFixedThreadPool(2)
val parSum: Par[Int] = Par.Unit(40)

// Without fusion: each .map wraps another layer, run evaluates both
parSum.map(_ + 2).map(_ * 2)
// => Map(Map(Unit(40), _ + 2), _ * 2)     // two Map nodes

parSum.map(_ + 2).map(_ * 2).run(es)
// => (_ * 2)((_ + 2)(40))                  // two function calls
// => 84

// With fusion: nested Maps are collapsed into one
parSum.map(_ + 2).map(_ * 2)
// => Map(Unit(40), (_ + 2) andThen (_ * 2)) // one Map node

parSum.map(_ + 2).map(_ * 2).run(es)
// => ((_ + 2) andThen (_ * 2))(40)          // single composed call
// => 84                                     // same result

es.shutdown()
```

**Look at `parallel/ParRep.scala`** for full mirror of `Par` but as a data type instead of a opaque type alias of a function.

The key insight: when `map` sees its argument is already a `Map`, it **composes the functions** (`g andThen f`) instead of nesting another `Map`. At evaluation time, `source` runs once and the composed function is applied in one step — no intermediate parallel computation needed.

| Concern | `Par` (opaque function) | `ParRep` (data type) |
|---------|------------------------|---------------------|
| What is a computation? | A function to call | Data to inspect |
| Optimization | Impossible (can't see inside) | Rewrite rules on the tree |
| `map` fusion | Can't detect chained maps | Pattern match and compose |
| When does execution happen? | When the function is called | When `run` interprets the tree |
| Multiple interpreters? | No (the function just runs) | Yes — debugger, tracer, mock, etc. |

A data type separates **description from execution**. The tree of operations can be inspected, rewritten, and optimized before `run` touches an `ExecutorService`. You could write a second interpreter that logs every step without changing a line of user code:

```scala
def traceRun(es: ExecutorService): Future[A] = this match
  case Unit(a) => println(s"Unit($a)"); UnitFuture(a)
  case Map(src, f) =>
    println("Map"); UnitFuture(f(src.traceRun(es).get))
  case Fork(src) =>
    println("Fork → submit"); src().traceRun(es)
  ...
```

The opaque function representation bakes execution into the closure itself — you get one path and no visibility. The data type representation makes the program a **first-class value**, which is the core idea behind free monads and the algebraic approach to library design that Part 3 explores further.

