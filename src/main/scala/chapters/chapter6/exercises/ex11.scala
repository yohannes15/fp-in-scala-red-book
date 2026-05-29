package chapters.chapter6.exercises

import datastructures.State

enum Input:
  case Coin, Turn

case class Machine(locked: Boolean, candies: Int, coins: Int)

object Machine:
  /**   - Inserting a coin into a locked machine will cause it to unlock if
    *     there’s any candy left.
    *   - Turning the knob on an unlocked machine will cause it to dispense
    *     candy and become locked.
    *   - Turning the knob on a locked machine or inserting a coin into an
    *     unlocked machine does nothing.
    *   - A machine that’s out of candy ignores all inputs.
    * @return
    *   the number of coins and candies left in the machine at the end .
    */
  def simulateMachine(inputs: List[Input]): State[Machine, (Int, Int)] =
    val stateActions = State.traverse(inputs)(i => State.modify(update(i)))
    stateActions.flatMap {
      _ => State.get.map(m => (m.coins, m.candies))
    }

  def simulateMachine2(inputs: List[Input]): State[Machine, (Int, Int)] =
    for
      _ <- State.traverse(inputs)(i => State.modify(update(i)))
      m <- State.get
    yield (m.coins, m.candies)

  /** Uses currying. It is benefical to at the defintion instead of doing at the
    * call site. Calling update(input) returns Machine => Machine which is what
    * State.modify wants.
    *
    * If we had non curried method instead like:
    *
    * {{{
    *   def update(i: Input, s: Machine): Machine = ???
    * }}}
    *
    * At the call site we would have to do
    *
    * {{{
    *   State.modify(_ => update(i))
    * }}}
    *
    * It is less verbose at the call site
    */
  val update = (i: Input) =>
    (m: Machine) =>
      (i, m) match
        case (_, Machine(_, 0, _))                    => m
        case (Input.Coin, Machine(false, _, _))       => m
        case (Input.Turn, Machine(true, _, _))        => m
        case (Input.Coin, Machine(true, candy, coin)) =>
          m.copy(false, candy, coin + 1)
        case (Input.Turn, Machine(false, candy, coin)) =>
          m.copy(true, candy - 1, coin)
