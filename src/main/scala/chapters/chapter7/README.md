# (Start of Part 2)

## Brief intro to Part 2

In part 1, we covered the fundamentals of FP and saw how the commitment to using only pure functions affects the basic building blocks of programs: loops, data structures, exceptions, and so on.

In part 2, we’ll see how the assumptions of FP we have built in part 1 affect library design. We’ll create three useful libraries in part 2 for:

- parallel and asynchronous computation
- testing programs
- parsing text.

The primary goal is developing skills in designing functional libraries, even for domains that look nothing like the ones here. There are no strict right answers in functional library design. Instead, we have a collection of design choices, each with different trade-offs. The goal is to understand these trade-offs and what different choices mean. 

Library design is not something only a select few people get to do; it’s part of the day-to-day work of ordinary functional programming. **In these chapters and beyond, absolutely feel free to experiment, play with different design choices, and develop your own aesthetic.**

As a final note, there are some repeated patterns of similar-looking code. Keep this in the back of mind. Part 3 covers how to remove this duplication, and discover an entire world of fundamental abstractions common to all libraries.

---

# Chapter 7

This chapter covers Purely functional parallelism

## Topics

- Developing a functional API for parallel computations
- Approaching APIs algebraically
- Defining generic combinators
