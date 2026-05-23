package datastructures

/** simple binary tree datastructure */
enum Tree[+A]:
  case Leaf(value: A)
  case Branch(left: Tree[A], right: Tree[A])

  def size: Int = this match
    case Leaf(_)      => 1
    case Branch(l, r) => l.size + r.size

// Branch(
//  Branch(Leaf("a"), Leaf("b")),
//  Branch(Leaf("c"), Leaf("d"))
// )
// =>
//          [][]
//    [][]          [][]
//[a]     [b]   [c]       [d]
//
