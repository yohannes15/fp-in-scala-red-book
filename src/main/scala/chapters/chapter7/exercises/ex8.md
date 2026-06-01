## Exercise 7.8

**Hard:** Look through the various static methods in `Executors` to get a feel for the different implementations of `ExecutorService` that exist. Then, before continuing, go back and revisit your implementation of `fork`, and try to find a counterexample or convince yourself that the law holds for your implementation.

See [executors](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executors.html)!

## Solution

Consider what happens when using a fixed thread pool with only a single thread. This is explored in greater detail in the `README.md` of this chapter in greater detail right after this exercise in the book
