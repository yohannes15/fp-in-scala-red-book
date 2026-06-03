# Chapter 8

This chapter covers **Property-based testing**

## Topics

- Verifying properties of APIs
- Developing a property-based testing library

In chapter 7, we discussed the idea that an API should form an **algebra**. An algebra is a collection of data types, functions over these data types and importantly laws or properties that express relationships between these functions. We also said it might be possible to somehow check these laws automatically.

This chapter takes us toward a simple but powerful library for **property-based testing**. The idea is decoupling the specification of program behaviour from the creation of test cases. Programmer focuses on specifying the behaviour of programs and giving high-level constraints on the test cases; the framework then automatically generates test cases that satisfy these constraints and runs tests to ensure programs behave as specified.

## Brief tour of property-based testing

Following is an example from [Scalacheck](https://github.com/typelevel/scalacheck), a property based testing library in Scala. A property looks something like this:

```scala
import org.scalacheck.{Gen, Prop}

val intList: Gen[List[Int]] = Gen.listOf(Gen.choose(0, 100))    //1
val prop =                                                      //2
  Prop.forAll(intList)(ns =>
    ns.reverse.reverse == ns) &&                                //3
  Prop.forAll(intList)(ns =>
    ns.headOption == ns.reverse.lastOption)                     //4
val failingProp = 
  Prop.forAll(intList)(ns => ns.reverse == ns)                  //5

prop.check // Ok, passed 100 tests.
failingProp.check // Falsified after 6 passed tests. ARG_0: List(0, 1)
```

1. A generator of lists of integers between 0 and 100
2. A property that specifies the behaviour of the List.reverse method
3. Check that reversing a list twice gives back the original list
4. Check that the first element becomes the last element after reversal
5. A property that is obviously false

`Gen[List[Int]]` is a generator that knows how to generate test data of the `List[Int]` type. We can sample from this generator, and it will produce lists of different lengths, filled with random numbers b/n 0 - 100. Generators in a property based testing library have a rich API. We can combine and compose generators in different ways, reuse them and so on.

The function `Prop.forAll` creates a *property* by combining a generator of type `Gen[A]` with some predicate of type `A => Boolean`. The property asserts that all values produced by the generator should satisfy the predicate. Properties have also rich API. You can combine props with &&. **A `Gen` objects generates a variety of different objects to pass to a Boolean expression, searching for one that will make it false.** 

When we invoke `prop.check`, ScalaCheck will randomly generate `List[Int]` values to try to find a case that falsifies the predicates we've supplied. Output shows either success (with number of generated objects tested) or failure with an example of the input that falsifed the predicate.

The goal of this sort of testing is not necessarily fully specifying program behavior but providing greater confidence in the code. Like testing in general, we can always make our properties more complete, but we should do the usual cost–benefit analysis to determine whether the additional work is worth doing.

Property-based testing libraries often come equipped with other useful features, following should provide an idea of what’s possible:

- **Test case minimization**: In the event of a failing test, the framework tries smaller sizes until it finds the smallest test case that also fails, which is more illuminating for debugging purposes. 

- **Exhaustive test case generation**: When the domain is small enough (for instance, if it’s all even integers less than 100), we may exhaustively test all its values, rather than generate sample values. If the property holds for all values in a domain, then we have an actual proof, rather than just the absence of evidence to the contrary.

## Choosing data types and functions


