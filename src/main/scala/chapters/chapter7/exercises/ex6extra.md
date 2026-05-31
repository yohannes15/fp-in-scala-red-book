Think of any other useful functions to write? Experiment with writing a few parallel computations of your own to see which ones can be expressed without additional primitives. 

Here are some ideas to try:

- Is there a more general version of the parallel summation function we wrote at the beginning of this chapter? Try using it to find the maximum value of an IndexedSeq in parallel.

- Write a function that takes a list of paragraphs (a `List[String]`) and returns the total number of words across all paragraphs in parallel. Look for ways to generalize this function.

- Implement `map3`, `map4`, and `map5` in terms of map2.
