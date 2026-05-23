package datastructures

/** simple binary tree datastructure */
enum Tree[+A]:
  case Leaf(value: A)
  case Branch(left: Tree[A], right: Tree[A])

  def size: Int = this match
    case Leaf(_)      => 1
    case Branch(l, r) => l.size + r.size

object Tree:
  /** extension methods for a more specific type! Tree[Int] */
  extension (t: Tree[Int])
    /** 1st positive integer in the tree, else the last visited value. */
    def firstPositive: Int = t match
      case Leaf(n)      => n
      case Branch(l, r) =>
        val lpos = l.firstPositive
        if lpos > 0 then lpos else r.firstPositive

    def maximum: Int = t match
      case Leaf(n)      => n
      case Branch(l, r) => l.maximum max r.maximum
