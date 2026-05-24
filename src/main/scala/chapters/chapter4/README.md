# Chapter 4

This chapter covers functional error handling. **Exceptions are side effects**. Instead we can represent failures and exceptions with ordinary values, **error as values**, and we can write higher-order functions that abstract out common patterns of error handling and recovery.

This is safer and retains referential transparency and through the use of HOFs we can preserve the primary benefit of exceptions: **consolidation of error-handling logic.**

In this chapter we will create the `Option` and `Either` types ourselves and even add some capability that isn't in the standard library.

## Topics

- Discussing the disadvantages of exceptions
- `Option` datatype
- `Either` datatype
- `Try` datatype

### Pros and Cons of exceptions

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

## Misc Notes

### Throw is an expression

```scala
val y: Int = throw Exception("fail!")
```

Because throw is an expression in Scala, it evaluates to the bottom type, `Nothing`. Since Nothing is a subclass of every other type, the compiler allows it to be assigned to an Int. However, any attempt to access the variable y will crash the program with an exception
