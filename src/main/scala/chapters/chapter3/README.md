# Chapter 3

This chapter covers functional data structures. 

## Topics

- Defining Functional data structures
- Pattern matching 
- Explaining data sharing
- Recursing over lists & generalizing to higher-order functions
- ADTs (algebaric data types)
- Exercises with writing and generaizling pure functions

## Chapter 2 Notes

### Functional Data Structures 

A functional data structure:
- is operated on using only pure functions 
- is immutable by definition
- most ubiquitous functional ds is `List` (singly linked list)

Just as functions can be polymorphic, data types can be as well, and by adding the type parameter `[+A]` after enum `List`, we declare the `List` datatype to be polymorphic in the type of elements it contains, which means we can use this same defintion for a list of `Int` elements or `String` or `Double`...

### Enum

An `enum` is a data type that consists of a series of `data constructors`, each defined by the `case` keyword. Enum is similar in Java or C, but in Scala we aren't limited to set of values (e.g, Red, Green, Blue), here we can have data constructors that take an arbitary arguments. 

Enums were introduced in scala3. In scala2 you would use a `sealed trait` and a series of `case object`s that extend/subtype the trait.

Refer to `datastructures/List.scala`

```scala
val ex1: List[Double] = List.Nil
val ex2: List[Int] = List.Cons(1, List.Nil)
val ex3: List[String] = List.Cons("a", List.Cons("b", List.Nil))
```

Each data constructor also introduces a pattern that can be used for pattern matching, as in the functions `sum` and `product`

### List

List exists in Scala standard library and in subsequent chapters we'll use that. The main difference betweent the `List` developed in datastructures/List.scala and the standard library version is that `Cons` is called `::`, which associates to the right, so: 

```scala
1 :: 2 :: Nil == 1 :: (2 :: Nil) == List(1, 2)
```

When pattern matching, `case Cons(h, t)` becomes `case h :: t`, which avoids having to nest parentheses if writing a pattern like `case h :: h2 :: t` to extract more than just the first element of the list.

There are a number of other useful methods on the standard library lists. These are defined as methods on `List[A]` rather than as standalone functions.

- `def take(n: Int): List[A]` - returns a list consisting of the first n elements of this.

- `def takeWhile(f: A => Boolean): List[A]` - returns a list consisting of the longest valid prefix of this whose elements all pass the predicate f.

- `def forall(f: A => Boolean): Boolean` - returns true if and only if all elements of this pass the predicate f.

- `def exists(f: A => Boolean): Boolean` - returns true if any element of this passes the predicate f.

- `scanLeft` and `scanRight` - These are similar to foldLeft and foldRight, but they return the List of partial results rather than just the final accumulated value.

Look at the Scala API documentation to see what other functions there are. If you find yourself writing an explicit recursive function for doing some sort of list manipulation, check the List API to see if something like the function you need already exists.

**Loss of efficiency when assembling list functions from simpler components**

One of the problems with `List` is that although we often express operations and algorithms in terms of general-purpose functions, the resulting implementation isn’t always efficient. we may end up making multiple passes over the same input or else have to write explicit recursive loops to allow early termination.

### Pattern Matching

Pattern matching works a bit like a fancy switch statement that may descend into the structure of the expression it examines and extract subexpressions of that structure. It’s introduced with an expression (the target or scrutinee) like `ds` followed by the keyword `match` and a sequence of `case`s.

Scala is often, but **NOT** always, able to determine at compile time if a match expression does not cover all cases. In such cases, the compiler reports a warning. Otherwise you get a runtime `MatchError`

A pattern matches the target if there exists an assignment of variables in the pattern to subexpressions of the target that make it structurally equivalent to the target. The resulting expression for a matching case will then have access to these variable assignments in its local scope.

### Data Sharing

When data is immutable, how do we write functions that, for ex, add or remove elements from a list? 

When we add an element 1 to the front of an existing list say `xs`, we return a new list - in this case, `Cons(1, xs)`. Since lists are immutable, we don't need to actually copy `xs`, we can just reuse it. This is called **data sharing**. Sharing of immutable data lets us implement functions more efficiently; we can always return immutable data strucutres without having to worry about subsequent code modifiying our data.

In the same way, to remove an element from the front of a list `val mylist = Cons(x, xs)`, we simply return its tail, `xs`. There's no real removing going on :) The original list, is still available and unharmed. Functional data structures are persistent, meaning existing references are never changed by operations on the data strucutre.

Writing purely functional data structures that support different operations efficiently is all about finding clever ways to exploit data sharing. 

As an example of what’s possible, in the Scala standard library, there’s a purely functional sequence implementation, [Vector](http://mng.bz/aZqm), with constant-time random access, updates, head, tail, init, and effectively constant-time additions to either the front or rear of the sequence.

If we look at `init` on list which returns list but with last element removed, we have to build up a copy of the entire list, as there's no structural sharing between the initial list and the result of init. Finally the implementation uses a stack frame for each element of the list, leading to potential stack overflow errors.

```scala
val l1 = List("a", "b", "c", "d")
val l2 = l1.tail // b -> rest
l1.tail == l2
// Both lists share the same data in memory. .tail doesn't modify l1. 
// It simply references  the tail of the original list. 
// Defense copying not needed. List is immutable
```

### Recursion over lists and generalizing to HOFs

`foldRight` is not specific to any one type of element, and we discover while generalizing that the value that’s returned doesn’t have to be of the same type as the elements of the list! One way of describing what `foldRight` does is that it replaces the constructors of the list, `Nil` and `Cons`, with `acc` and `f`.

```scala
Cons(1, Cons(2, Nil))
f   (1, f   (2, acc))
```

```scala

foldRight(Cons(1, Cons(2, Cons(3, Nil))), 0, (x,y) => x + y)
1 + foldRight(Cons(2, Cons(3, Nil)), 0, (x,y) => x + y)      // ①
1 + (2 + foldRight(Cons(3, Nil), 0, (x,y) => x + y))
1 + (2 + (3 + (foldRight(Nil, 0, (x,y) => x + y))))
1 + (2 + (3 + (0)))
6
// ① Replace foldRight with its definition.
```

Note that foldRight must traverse all the way to the end of the list (pushing frames onto the call stack as it goes) before it can begin collapsing it. 

### foldLeft

`foldLeft` is the tail-recursive counterpart to `foldRight`. It processes the list left-to-right, consuming the accumulator as it goes, so it doesn't need to push stack frames:

```scala
foldLeft(Cons(1, Cons(2, Cons(3, Nil))), 0, (acc, x) => acc + x)
foldLeft(Cons(2, Cons(3, Nil)), 0 + 1, (acc, x) => acc + x)      // ①
foldLeft(Cons(3, Nil), (0 + 1) + 2, (acc, x) => acc + x)
foldLeft(Nil, ((0 + 1) + 2) + 3, (acc, x) => acc + x)
((0 + 1) + 2) + 3
6
// ① f takes (acc, x) order — the accumulator comes first
```

Key differences from `foldRight`:

| | foldRight | foldLeft |
|---|---|---|
| **Traversal** | Right-to-left (via stack) | Left-to-right (tail-recursive) |
| **Stack safe?** | No (stack grows with list) | Yes (`@annotation.tailrec`) |
| **f parameter order** | `(A, B) => B` — element first | `(B, A) => B` — accumulator first |
| **Algebraic view** | Replaces `Cons` with `f`, `Nil` with `acc` | Same, but associates to the left |
| **Natural use cases** | Building structures (like `append`, `map`) | Aggregations (like `sum`, `length`) |

#### Right-to-left operations need foldRight (or reverse + foldLeft)

Some operations, like `append`, must preserve list order. `foldRight` handles this naturally because it processes elements in their original sequence. The same operation via `foldLeft` would reverse the result, so you need to `reverse` the input first:

```scala
// foldRight — natural: Cons(1, Cons(2, ys))
append(xs, ys) = foldRight(xs, ys, Cons(_, _))

// foldLeft — need reverse first
append(xs, ys) = foldLeft(reverse(xs), ys, (acc, a) => Cons(a, acc))
```

This pattern (reverse then foldLeft) is how `foldRightViaFoldLeft` works in `List.scala` — it gives us the right-to-left semantics of `foldRight` with the stack safety of `foldLeft`.

## ADT

A datatype defined by one or more data constructors, each of which may contain zero or more arguments. The datatype is the sum/union of its data constructors, and each data constructor is the product of its arguments.

The naming is not coincidental. There’s a deep connection, beyond the scope of this book, between the “addition” and “multiplication” of types to form an ADT and the addition and multiplication of numbers. Algebraic data types can be used to define other data structures. (Example: List, Binary Tree ...)

## Tuple in Scala

Pairs and tuples of higher arities (e.g., triples) are also algebraic data types. They work justlike the ADTs we’ve been writing here but have special syntax:

```scala
scala> val p = ("Bob", 42)
val p: (String, Int) = (Bob,42)
scala> p(0)
val res0: String = Bob
scala> p(1)
val res1: Int = 42
scala> p match { case (a, b) => b }
val res2: Int = 42
```

In this example, ("Bob", 42) is a pair whose type is (String, Int), which is syntactic sugar for`Tuple2[String, Int]`. We can extract the first or second element of this pair by index, and we can pattern match on this pair much like any other case class. If we try passing an invalid index—e.g., 3 or -1—we get a **compilation error, not a runtime** error. Higher arity tuples work similarly.

## Misc Notes

### Variance

`+A` in `List[+A]` means `List` is covariant in its element type. If `Cat` is a subtype of `Animal`, then `List[Cat]` is also a subtype of `List[Animal]`.

This is safe because our `List` is immutable: we can read `A` values out, but we do not mutate the list in place.

It’s certainly possible to write code without using variance annotations at all, and function signatures sometimes end up simpler (whereas type inference often gets worse). Use variance annotations where it’s convenient to do so, but you should feel free to experiment with both approaches.

### * Symbol (varargs)

The `*` symbol is used to denote variadic parameters (often called "varargs"). It allows a function to accept a variable number of arguments of a specific type, ranging from zero to infinitely many. 

When you place * after a type in a parameter declaration (e.g., `String*`), Scala automatically packages all the passed arguments into an immutable sequence (usually a `Seq`) inside the function body.

For data types, it’s a common idiom to have a variadic apply method in the companion object to conveniently construct instances of the data type.

Variadic functions just provide a little syntactic sugar for creating and passing a Seq of elements explicitly. [Seq](http://mng.bz/f4k9) is the interface in Scala’s collections library implemented by sequence-like data structures, such as lists, queues, and vectors. Contains the `head` and `tail`. The `*` passes a `Seq` to the variadic method. 

```scala
def printNames(names: String*): Unit = 
  names.foreach(println)

printNames("Alice", "Bob") 
printNames("Charlie") 
printNames() // Allowed, passes an empty sequence
```

### Random notes

In data constructors (enum), `Cons` is short for `construct`.

```scala
List("a", "b") == Cons("a", Cons("b", Nil))
```

---

Scala generates a default `def toString: String` method for enumerations, which can be convenient for debugging. You can see the output of this default toString implementation if you experiment with List values in the REPL, which uses toString to render the result of each expression. 

`List.Cons(1, List.Nil)` will be printed as the string "Cons(1, Nil)", for instance. But note that the generated toString will be naively recursive and will cause stack overflow when printing long lists, so you may wish to provide a different implementation.

---

A common convention is to use `xs, ys, as, or bs` as variable names for a sequence of some sort and `x, y, a, or b` as the name for a single element of a sequence. Another common naming convention is `h or hd` for the first element of a list (the head of the list), `t or tl` for the remaining elements (the tail), and `l` for an entire list.

---

Companion objects have access to private and protected members of the type with the same name but are otherwise like any other object.

---

In Scala, all methods whose names end in : are right associative. That is, the expression `x :: xs` is actually the method call `xs.::(x)`, which in turn calls the data constructor `::(x,xs)`.
