# Chapter 6

This chapter covers Purely functional state

## Topics

- Discussing purely functional random number generation
- Working with stateful APIs purely functionally
- Writing purely functional programs that manipulate state
- Introducing the `State` data type

## Generating random numbers using side effects

There is a class in std library called `scala.util.Random`, with a pretty typical imperative API that relies on side effects. 

```scala
val rng = new scala.util.Random // creates a new rng seeded with the current system time
rng.nextDouble // 0.9867076608154569
rng.nextDouble // 0.8455696498024141
rng.nextInt // -623297295
rng.nextInt(10) // 4  // gets a random integer b/n 0 and 9
```

Even if we don’t know anything about what happens inside `scala.util.Random`, we can assume the object `rng` has some internal state that gets updated after each invocation, since we’d otherwise get the same value each time we called `nextInt` or `nextDouble`. Because the state updates are performed as a side effect, these methods aren’t referentially transparent. And as we know, this implies that they aren’t as testable, composable, modular, and easily parallelized as they could be. Lets say we had the following method:

```scala
def rollDie: Int = 
  val rng = new scala.util.Random
  rng.nextInt(6)    // Returns a random number from 0 to 5 (a BUG!!)
```

Note that whats important here is not this specific bug, but we can easily image a much more complicated method and the bug far more subtle. The more complex the program and the subtler the bug, the more important it is to be able to reproduce bugs in a reliably way. One suggestion might be passing in the rng, that way, we can pass the same generator that caused the test to fail

```scala
def rollDie(rng: scala.util.Random): Int = rng.nextInt(6)
```

But theres a problem: the same generator has to be both created with the same seed and be in the same state (methods have been called a certain number of times since creation). That is hard because every time we call `nextInt` for ex, the previous state of `rng` is destroyed. Do we now need a separate mechanism to keep track of how many times we’ve called the methods on Random? **No, we should avoid side effects on principle.**

## Purely functional random number generation

The key to recovering referential transparency is making the state updates *explicit*. Don't update the state as a side effect, but simply return the new state along with the value we're generating. Here's one possible interface for a rng:

```scala
trait RNG:
  def nextInt: (Int, RNG)
```

In effect, we separate the concern of computing what the next state is from the concern of communicating the new state to the rest of the program. Old state is unmodified. This leaves the caller of `nextInt` with complete control of what to do with the new state. Note that we're still *encapsulating* the state in the sense that users of this API don't know anything about the implementation of the rng itself.

But we do need to have an implementation, so lets pick a simple one. This rng uses the same algorithm as `Random`, which is called a [linear congruential generator](http://mng.bz/r046).

```scala
case class SimpleRNG(seed: Long) extends RNG:
  def nextInt: (Int, RNG) = 
    val newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL // #1
    val nextRNG = SimpleRNG(newSeed)                             // #2
    val n = (newSeed >>> 16).toInt                               // #3
    (n, nextRNG)                                                 // #4

// 1. & is a bitwise AND. We use the current seed to generate a new seed
// 2. The next state
// 3. >>> is right binary shift with zero fill. The value n is the new pseduo-random integer
// 4. return value is a tuple containing both the rando and the next RNG state.

val rng = SimpleRNG(42) // choosing arbitrary seed value 42
val (n1, rng2) = rng.nextInt
// n1: Int = 16159453
// rng2: RNG = SimpleRNG(1059025964525))
val (n2, rng3) = rng2.nextInt
// n2: Int = -1281479697
// rng3: RNG = SimpleRNG(197491923327988)

ef randomPairSame(rng: RNG): (Int, Int) =
  val (i1, _) = rng.nextInt
  val (i2, _) = rng.nextInt
  (i1, i2) // here i1 and i2 will be the same

def randomPairDiff(rng: RNG): ((Int, Int), RNG) =
  val (i1, rng2) = rng.nextInt
  val (i2, rng3) = rng2.nextInt
   ((i1, i2), rng3) 

// i1 and i2 two distinct numbers, rng3 is next state for caller
```

We can run this sequence of statements as many times as we want, and we'll always get the same values. When we call `rng.nextInt`, it will always return `16159453` and a new RNG, whose nextInt will always return `-1281479697`. In others words, this API is pure!

You can see the general pattern, and perhaps you can also see how it might get tedious to use this API directly. Mp worries there is a better API for state actions. 

## Making stateful APIs pure

State problems isn't unique to rng. It comes up frequently. Another example is shown below, but **note:* an efficiency loss comes with computing next states using pure functions because it means we can't actually mutate the data in place. It can be mitigated by using efficient purely functional ds. It's also possible in some cases to mutate the data in place without breaking referential transparency (more in chapter 4).

```scala
class Foo:
  private var s: FooState = ...
  def bar: Bar
  def baz: Int
```

Suppose `bar` and `baz` each mutate `s` in some way. We can translate this to purely functional API by making the transition from one state to the next explicit:

```scala
trait Foo:
  def bar: (Bar, Foo)
  def baz: (Int, Foo)
```

## Better API for state actions 

Looking back at our implementations, we’ll notice a common pattern: each of our functions has a type of the form `RNG => (A, RNG)` for some type `A`. Functions of this type are called **state actions** or **state transitions** because they transform RNG states from one to the next. These state actions can be combined in various ways to generate new state actions. It's pretty tedious and repetitive to pass the state along ourselves; we will define functions to pass the state from one action to the next automatically!

Lets make a type alias to simplify our thinking about type of actions. 

```scala
type Rand[+A] = RNG => (A, RNG)
// We can now turn methods such as `nextInt` into values of this new type
val int: Rand[Int] = rng => rng.nextInt
```

We can think of a value of type `Rand[A]` as a *randomly generated A*, although that's not really accurate. It's really a *state action* - a program that depends on some RNG, uses it to generate an `A`, and transitions the RNG to a new state that can be used by another action later. We want to write functions that let us combine `Rand` state actions, while avoiding passing along the RNG state expliclity.

For example, a simple RNG state transition is the *unit* action, which passes the RNG state without using it, always returning a constant value rather than a random value. 

```scala
def unit[A](a: A): Rand[A] =
  rng => (a, rng)
```

There is also a map for transforming the output of a state action without modifying the resultant state.

```scala
def map[A, B](s: Rand[A])(f: A => B): Rand[B] = 
  rng =>
    val (a, rng2) = s(rng)
    (f(a), rng2)

// Example of how map is used. here's nonNegativeEven, which reuses nonNegativeInt to generate an Int thats greater than or equal to zero and divisibly by two
def nonNegativeEven: Rand[Int] = 
  map(nonNegativeInt)(i => i - (i % 2))
```

## Misc Notes

### Trait

A `trait` is an abstract interface that may optionally contain implementations of some methods. Key features:

- Multiple Inheritance -> a class can extend any number of traits.
- Abstract & Concrete Members 
- No Instantiation -> Traits cannot be instantiated on their own
- Trait Parameters -> Introduced in Scala3

### Dealing with awkwardness in functional programming

As you write more functional programs, you’ll sometimes encounter situations where the functional way of expressing a program feels awkward or tedious. Does this imply that purity is the equivalent of trying to write an entire novel without using the letter E? Of course not. Awkwardness like this is almost always a sign of some missing abstraction waiting to be discovered.

When you encounter these situations, plow ahead and look for common patterns you can factor out. Most likely, this is a problem others have encountered, and even rediscover the standard solution. Even if you get stuck, struggling to puzzle out a clean solution yourself will help you better understand what solutions others have discovered for dealing with similar problems.

With practice, experience, and more familiarity with the idioms contained in this book, expressing a program functionally will become effortless and natural. Of course, good design is still hard, but programming using pure functions greatly simplifies the design space.
