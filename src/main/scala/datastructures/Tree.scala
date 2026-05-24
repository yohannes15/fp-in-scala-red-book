package datastructures

/** simple binary tree datastructure */
enum Tree[+A]:
  case Leaf(value: A)
  case Branch(left: Tree[A], right: Tree[A])

  def size: Int = fold(a => 1, (l, r) => 1 + l + r)

  def depth: Int = fold(a => 0, (l, r) => 1 + l.max(r))

  def map[B](f: A => B): Tree[B] =
    fold(a => Leaf(f(a)), (l, r) => Branch(l, r))

  def fold[B](f: A => B, g: (B, B) => B): B = this match
    case Leaf(a)      => f(a)
    case Branch(l, r) => g(l.fold(f, g), r.fold(f, g))

object Tree:
  extension (t: Tree[Int])
    def maximum: Int =
      t.fold(a => a, (l, r) => l max r)

    /**   - if left subtree already found a positive → keep it
      *   - otherwise check right subtree
      *   - if neither found one → return the last visited value (r._2)
      */
    def firstPositive: Int =
      t.fold[(Boolean, Int)](
        a => (a > 0, a),
        (l, r) =>
          if l._1 then l
          else if r._1 then r
          else (false, r._2)
      )._2
