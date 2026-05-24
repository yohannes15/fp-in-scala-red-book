## Exercise 4.1

Implement `map`, `flatMap`, `getOrElse`, `orElse` and `filter` function on Option. As you implement each function, try to think about what it means and in what situations you’d use it. We’ll explore when to use each of these functions next. Here are a few hints for solving this exercise:

- It’s fine to use pattern matching, though you should be able to implement all the functions besides map and getOrElse without resorting to pattern matching. 

- Try implementing `flatMap`, `orElse`, and `filter` *in terms of map and getOrElse*.

- For `map` and `flatMap`, the type signature should be enough to determine the implementation.

- `getOrElse` returns the result inside the Some case of the Option, or if the Option is None, it returns the given default value.

- `orElse` returns the first Option if it’s defined; otherwise, it returns the second Option.

## Solution

Look at datastrucutres/Option.scala
