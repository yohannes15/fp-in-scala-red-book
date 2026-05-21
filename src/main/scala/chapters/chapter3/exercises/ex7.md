## Exercise 3.7

Can product, implemented using `foldRight`, immediately halt the recursion and return `0.0` if it encounters a `0.0`? Why or why not? Consider how any short circuiting might work if you call `foldRight` with a large list.

## Solution

No, it can't because foldRight needs to go all the way to the end of the list before it startings "folding" results back. Once it gets to end of the list is only when it starts calculating results back up.

