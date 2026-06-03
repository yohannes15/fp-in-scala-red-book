package properties

import org.scalacheck.{Gen, Prop}

@main def propertyTestsForSumListInt: Unit =
  val genList = Gen.listOf(Gen.choose(0, 100))
  val genN = Gen.choose(0, 100)
  val genSameValueList =
    for
      x <- Gen.choose(0, 100)
      n <- genN
    yield (x, List.fill(n)(x))

  val prop = Prop(List.empty[Int].sum == 0) &&
    Prop.forAll(genSameValueList)((x, ns) =>
      ns.sum == ns.size * x
    ) && Prop.forAll(genList)(ns =>
      ns.reverse.sum == ns.sum
    ) && Prop.forAll(genList)(ns =>
      val (left, right) = ns.splitAt(ns.size / 2)
      left.sum + right.sum == ns.sum
    ) && Prop.forAll(genN)(n =>
      (1 to n).toList.sum == n * (n + 1) / 2
    )
  prop.check()

@main def propertyTestsForMaximumListInt: Unit =
  val genInt = Gen.choose(0, 100)
  val genNonEmptyList = Gen.nonEmptyListOf(genInt)
  val genSameValueList =
    for
      x <- genInt
      size <- Gen.choose(1, 100)
    yield (x, List.fill(size)(x))

  val prop = Prop {
    try
      val _ = List.empty[Int].max
      false
    catch
      case _: UnsupportedOperationException => true
  } && Prop.forAll(genInt)(n =>
    List(n).max == n
  ) && Prop.forAll(genSameValueList)(ns =>
    val (x, list) = ns
    list.max == x
  ) && Prop.forAll(genNonEmptyList)(ns =>
    val maximum = ns.max
    ns.contains(maximum)
  ) && Prop.forAll(genNonEmptyList)(ns =>
    val maximum = ns.max
    ns.forall(_ <= maximum)
  )
  prop.check()
