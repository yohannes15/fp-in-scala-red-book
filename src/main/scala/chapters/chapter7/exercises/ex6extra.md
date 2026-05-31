## Exercise 7.6 Extra

Think of any other useful functions to write? Experiment with writing a few parallel computations of your own to see which ones can be expressed without additional primitives. 

Here are some ideas to try:

- Is there a more general version of the parallel summation function we wrote at the beginning of this chapter? Try using it to find the maximum value of an IndexedSeq in parallel.

- Write a function that takes a list of paragraphs (a `List[String]`) and returns the total number of words across all paragraphs in parallel. Look for ways to generalize this function.

- Implement `map3`, `map4`, and `map5` in terms of map2.

## Solutions

Look at parallel/Par.scala.

- Yes there is, we can right a `parFold` and implement `max` using `parFold(ints)(MinValue)(_ max _)`
- Implemented using `ParMap` to map over and convert each String into a Int and then to combine those Ints for a total sum. From this we realized we can invent `parFoldMap`, which encapsulates, the logic of mapping over, tranforming each element and then combining them. Using this we can implement `parFold` as well (which skips the transformation)
- Implemented `map3` using currying on the first map2 between pa and pb to produce a `Par[C => D]` and then used than on map2 between the combination and pc to return pd
