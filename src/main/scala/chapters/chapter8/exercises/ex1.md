## Exercise 8.1

To get used to thinking about testing in this way, come up with properties that specify the implementation of a sum: `List[Int] => Int` function. You don’t have to write your properties down as executable ScalaCheck code—an informal description is fine. Here are some ideas to get you started:

- Reversing a list and summing it should provide the same result as summing the original, nonreversed list.
- What should the sum be if all elements of the list are of the same value?
- Can you think of other properties?

## Solution

Look at test/scala/properties/PropertyEx.scala
