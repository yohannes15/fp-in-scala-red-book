package datastructures

/** simple binary tree datastructure */
enum Tree[+A]:
  case Leaf(value: A)
  case Branch(left: Tree[A], right: Tree[A])

  def size: Int = this match
    case Leaf(_)      => 1
    case Branch(l, r) => l.size + r.size

  def depth: Int = this match
    case Leaf(_)      => 0
    case Branch(l, r) => 1 + (l.depth max r.depth)

  def map[B](f: A => B): Tree[B] = this match
    case Leaf(a)      => Leaf(f(a))
    case Branch(l, r) => Branch(l.map(f), r.map(f))

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
