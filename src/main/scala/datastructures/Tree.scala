package datastructures

/** simple binary tree datastructure */
enum Tree[+A]:
  case Leaf(value: A)
  case Branch(left: Tree[A], right: Tree[A])

  def size: Int = this match
    case Leaf(_)      => 1
    case Branch(l, r) => l.size + r.size

object Tree:
  /** extension methods for a more specific type! */
  extension (t: Tree[Int])
    /** returns the first positive integer in the tree. if none exists, returns
      * the last visited value.
      */
    def firstPositive: Int = t match
      case Leaf(i)      => i
      case Branch(l, r) =>
        val lpos = l.firstPositive
        if lpos > 0 then lpos else r.firstPositive
