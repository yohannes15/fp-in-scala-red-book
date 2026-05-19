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
