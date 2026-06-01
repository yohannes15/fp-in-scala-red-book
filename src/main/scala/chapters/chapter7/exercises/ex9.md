## Exercise 7.9

**Hard:** Show that any fixed-size thread pool can be made to deadlock given curr implementation of `fork`.

## Solution

Any fixed-size thread pool can be deadlocked by running an expression of the form `fork(fork(fork(x)))`, where **there’s at least one more fork than there are threads in the pool. Each thread in the pool blocks on the call to .get, resulting in all threads being blocked, while one more logical thread is waiting to run and, hence, resolve all the waiting.**

For a thread pool of size 2, `fork(fork(fork(x)))` will deadlock, and so on. Another, perhaps more interesting example is `fork(map2(fork(x), fork(y)))`. In this case, the outer task is submitted first and occupies a thread waiting for both `fork(x)` and `fork(y)`. The `fork(x)` and `fork(y)` tasks are submitted and run in parallel, except that only one thread is available, resulting in **deadlock.**


