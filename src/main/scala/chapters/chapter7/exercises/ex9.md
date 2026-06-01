## Exercise 7.9

**Hard:** Show that any fixed-size thread pool can be made to deadlock given curr implementation of `fork`.

## Solution

Any fixed-size thread pool can be deadlocked by running an expression of the form `fork(fork(fork(x)))`, where **there’s at least one more fork than there are threads in the pool. Each thread in the pool blocks on the call to .get, resulting in all threads being blocked, while one more logical thread is waiting to run and, hence, resolve all the waiting.**


